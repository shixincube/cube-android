/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.messaging;

import android.util.Log;
import android.util.MutableBoolean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cube.contact.ContactService;
import cube.contact.ContactServiceEvent;
import cube.contact.handler.ContactHandler;
import cube.contact.model.Contact;
import cube.contact.model.Self;
import cube.core.Hook;
import cube.core.Module;
import cube.core.Packet;
import cube.core.handler.CompletionHandler;
import cube.messaging.extension.MessageTypePlugin;
import cube.messaging.hook.InstantiateHook;
import cube.messaging.model.Message;
import cube.util.ObservableEvent;

/**
 * 消息传输服务。
 */
public class MessagingService extends Module {

    public final static String NAME = "Messaging";

    private long retrospectDuration = 30L * 24L * 60L * 60000L;

    private MessagingPipelineListener pipelineListener;

    private MessagingStorage storage;

    private MessagingObserver observer;

    private ContactService contactService;

    private boolean ready;

    private long lastMessageTime;

    private Timer pullTimer;
    private CompletionHandler pullCompletionHandler;

    public MessagingService() {
        super(MessagingService.NAME);
        this.pipelineListener = new MessagingPipelineListener(this);
        this.storage = new MessagingStorage(this);
        this.observer = new MessagingObserver(this);
        this.ready = false;
        this.lastMessageTime = 0;
    }

    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }

        // 组装插件
        this.assemble();

        this.pipeline.addListener(MessagingService.NAME, this.pipelineListener);

        // 监听联系人模块
        this.contactService = (ContactService) this.kernel.getModule(ContactService.NAME);
        this.contactService.attachWithName(ContactServiceEvent.SelfReady, this.observer);
        this.contactService.attachWithName(ContactServiceEvent.SignIn, this.observer);
        this.contactService.attachWithName(ContactServiceEvent.SignOut, this.observer);

        synchronized (this) {
            if (null != this.contactService.getSelf() && !this.ready) {
                this.prepare(new CompletionHandler() {
                    @Override
                    public void handleCompletion(Module module) {
                        ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Ready, MessagingService.this);
                        notifyObservers(event);
                    }
                });
            }
        }

        return true;
    }

    @Override
    public void stop() {
        super.stop();

        // 拆除插件
        this.dissolve();

        this.contactService.detachWithName(ContactServiceEvent.SelfReady, this.observer);
        this.contactService.detachWithName(ContactServiceEvent.SignIn, this.observer);
        this.contactService.detachWithName(ContactServiceEvent.SignOut, this.observer);

        this.pipeline.removeListener(MessagingService.NAME, this.pipelineListener);

        // 关闭存储
        this.storage.close();
    }

    @Override
    public boolean isReady() {
        return this.ready;
    }

    private void assemble() {
        this.pluginSystem.addHook(new InstantiateHook());

        // 注册插件
        this.pluginSystem.registerPlugin(InstantiateHook.NAME, new MessageTypePlugin());
    }

    private void dissolve() {
        this.pluginSystem.clearHooks();
        this.pluginSystem.clearPlugins();
    }

    /**
     * 获取最近的消息清单，返回的每条消息都来自不同的会话联系人或群组。
     * 最大数量 50 条记录。
     *
     * @return 返回消息列表。如果返回 {@code null} 值表示消息服务模块未启动。
     */
    public List<Message> getRecentMessages() {
        return this.getRecentMessages(50);
    }

    /**
     * 获取最近的消息清单，返回的每条消息都来自不同的会话联系人或群组。
     *
     * @param maxLimit 指定最大记录数量。
     * @return 返回消息列表。如果返回 {@code null} 值表示消息服务模块未启动。
     */
    public List<Message> getRecentMessages(int maxLimit) {
        if (!this.hasStarted()) {
            return null;
        }

        List<Message> list = this.storage.queryRecentMessages(maxLimit);
        if (list.isEmpty()) {
            return list;
        }

        // 调用插件
        Hook<Message> hook = (Hook<Message>) this.pluginSystem.getHook(InstantiateHook.NAME);
        List<Message> result = new ArrayList<>(list.size());
        for (Message message : list) {
            Message compMessage = hook.apply(message);
            result.add(compMessage);
        }

        return result;
    }

    private void prepare(CompletionHandler handler) {
        Self self = this.contactService.getSelf();

        // 开启存储
        this.storage.open(this.getContext(), self.id, self.domain);

        long now = System.currentTimeMillis();

        // 查询本地最近消息时间
        long time = this.storage.queryLastMessageTime();
        if (0 == time) {
            this.lastMessageTime = now - this.retrospectDuration;
        }
        else {
            this.lastMessageTime = time;
        }

        // 服务就绪
        this.ready = true;

        // 从服务器上拉取自上一次时间戳之后的所有消息
        this.queryRemoteMessage(this.lastMessageTime + 1, now, new CompletionHandler() {
            @Override
            public void handleCompletion(Module module) {
                handler.handleCompletion(MessagingService.this);
            }
        });
    }

    private void queryRemoteMessage(long beginning, long ending, CompletionHandler completionHandler) {
        // 如果没有网络直接回调函数
        if (!this.pipeline.isReady()) {
            completionHandler.handleCompletion(this);
            return;
        }

        if (null != this.pullTimer) {
            return;
        }

        this.pullTimer = new Timer();
        this.pullTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                completionHandler.handleCompletion(MessagingService.this);

                try {
                    pullTimer.cancel();
                } catch (Exception e) {
                    // Nothing
                }

                pullTimer = null;
            }
        }, 10L * 1000L);

        this.pullCompletionHandler = completionHandler;

        Self self = this.contactService.getSelf();

        JSONObject payload = new JSONObject();
        try {
            payload.put("id", self.id.longValue());
            payload.put("domain", self.domain);
            payload.put("device", self.device.toJSON());
            payload.put("beginning", beginning);
            payload.put("ending", ending);
        } catch (JSONException e) {
            // Nothing
        }

        // 向服务器请求数据
        Packet packet = new Packet(MessagingAction.Pull, payload);
        this.pipeline.send(MessagingService.NAME, packet);
    }

    protected void fireContactEvent(ObservableEvent event) {
        if (this.pipeline.isReady()) {
            if (event.name.equals(ContactServiceEvent.SignIn)) {
                synchronized (this) {
                    if (!this.ready) {
                        // 准备数据
                        this.prepare(new CompletionHandler() {
                            @Override
                            public void handleCompletion(Module module) {
                                ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Ready, MessagingService.this);
                                notifyObservers(event);
                            }
                        });
                    }
                }
            }
            else if (event.name.equals(ContactServiceEvent.SignOut)) {
                // TODO
            }
        }
        else {
            if (event.name.equals(ContactServiceEvent.SelfReady)) {
                synchronized (this) {
                    if (!this.ready) {
                        // 准备数据
                        this.prepare(new CompletionHandler() {
                            @Override
                            public void handleCompletion(Module module) {
                                ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Ready, MessagingService.this);
                                notifyObservers(event);
                            }
                        });
                    }
                }
            }
        }
    }

    protected void triggerNotify(JSONObject data) {
        Message message = null;
        try {
            message = new Message(data, this);
        } catch (JSONException e) {
            Log.w("MessagingService", "#triggerNotify", e);
        }

        // 填充消息相关实体对象
        this.fillMessage(message);

        // 数据写入数据库
        boolean exists = this.storage.updateMessage(message);

        if (message.getRemoteTimestamp() > this.lastMessageTime) {
            this.lastMessageTime = message.getRemoteTimestamp();
        }

        if (!exists) {
            // 调用插件
            Hook<Message> hook = (Hook<Message>) this.pluginSystem.getHook(InstantiateHook.NAME);
            Message compMessage = hook.apply(message);

            ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Notify, compMessage);
            this.notifyObservers(event);
        }
    }

    protected void triggerPull(int code, JSONObject data) {
        if (null != this.pullTimer) {
            this.pullTimer.cancel();

            // 触发回调，通知应用已收到服务器数据
            if (null != this.pullCompletionHandler) {
                this.execute(new Runnable() {
                    @Override
                    public void run() {
                        pullCompletionHandler.handleCompletion(MessagingService.this);
                        pullCompletionHandler = null;
                    }
                });
            }

            this.pullTimer = null;
        }

        try {
            int total = data.getInt("total");
            long beginning = data.getLong("beginning");
            long ending = data.getLong("ending");
            JSONArray messages = data.getJSONArray("messages");

            Log.d("MessaginService", "Pull messages total: " + total);

            for (int i = 0; i < messages.length(); ++i) {
                JSONObject json = messages.getJSONObject(i);
                this.triggerNotify(json);
            }

            // 对消息进行状态对比
            // 如果服务器状态与本地状态不一致，将服务器上的状态修改为本地状态
        } catch (JSONException e) {
            // Nothing
        }
    }

    protected void fillMessage(Message message) {
        Self self = this.contactService.getSelf();
        message.setSelfTyper(message.getFrom() == self.id.longValue());

        MutableBoolean gotFrom = new MutableBoolean(false);
        MutableBoolean gotToOrSource = new MutableBoolean(false);

        CompletionHandler handler = (module) -> {
            if (gotFrom.value && gotToOrSource.value) {
                synchronized (message) {
                    message.notify();
                }
            }
        };

        this.contactService.getContact(message.getFrom(), new ContactHandler() {
            @Override
            public void handleContact(Contact contact) {
                message.setSender(contact);
                gotFrom.value = true;
                handler.handleCompletion(MessagingService.this);
            }
        });

        if (message.isFromGroup()) {
            // TODO 获取群组
            gotToOrSource.value = true;
            handler.handleCompletion(MessagingService.this);
        }
        else {
            this.contactService.getContact(message.getTo(), new ContactHandler() {
                @Override
                public void handleContact(Contact contact) {
                    message.setReceiver(contact);
                    gotToOrSource.value = true;
                    handler.handleCompletion(MessagingService.this);
                }
            });
        }

        synchronized (message) {
            try {
                message.wait(3000L);
            } catch (InterruptedException e) {
                // Nothing
            }
        }
    }
}
