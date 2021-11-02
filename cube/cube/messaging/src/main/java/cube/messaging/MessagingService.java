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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import cube.contact.ContactService;
import cube.contact.ContactServiceEvent;
import cube.contact.handler.ContactHandler;
import cube.contact.model.Contact;
import cube.contact.model.Self;
import cube.core.Hook;
import cube.core.Module;
import cube.core.Packet;
import cube.core.PipelineState;
import cube.core.handler.CompletionHandler;
import cube.core.handler.PipelineHandler;
import cube.messaging.extension.MessageTypePlugin;
import cube.messaging.hook.InstantiateHook;
import cube.messaging.model.Conversation;
import cube.messaging.model.ConversationState;
import cube.messaging.model.ConversationType;
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

    private MessagingRecentEventListener recentEventListener;

    private List<MessageEventListener> eventListeners;

    private Map<Long, List<MessageEventListener>> conversationMessageListeners;

    private AtomicBoolean preparing;

    private boolean ready;

    private long lastMessageTime;

    private Timer pullTimer;
    private CompletionHandler pullCompletionHandler;

    private List<Conversation> conversations;

    /**
     * 会话对应的消息列表。
     */
    private Map<Long, MessageList> conversationMessageListMap;

    public MessagingService() {
        super(MessagingService.NAME);
        this.pipelineListener = new MessagingPipelineListener(this);
        this.storage = new MessagingStorage(this);
        this.observer = new MessagingObserver(this);
        this.preparing = new AtomicBoolean(false);
        this.ready = false;
        this.lastMessageTime = 0;
        this.conversationMessageListMap = new ConcurrentHashMap<>();
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
            if (null != this.contactService.getSelf() && !this.ready && !this.preparing.get()) {
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

    /**
     * 设置消息服务最近事件监听器。
     *
     * @param listener 监听器。
     */
    public void setRecentEventListener(MessagingRecentEventListener listener) {
        this.recentEventListener = listener;
    }

    /**
     * 添加消息事件监听器。
     *
     * @param listener 消息监听器。
     */
    public void addEventListener(MessageEventListener listener) {
        if (null == this.eventListeners) {
            this.eventListeners = new Vector<>();
        }

        if (!this.eventListeners.contains(listener)) {
            this.eventListeners.add(listener);
        }
    }

    /**
     * 移除消息事件监听器。
     *
     * @param listener 消息监听器。
     */
    public void removeEventListener(MessageEventListener listener) {
        if (null == this.eventListeners) {
            return;
        }

        this.eventListeners.remove(listener);
    }

    /**
     * 添加指定会话的消息事件监听器。
     *
     * @param conversation 指定会话。
     * @param listener 消息事件监听器。
     */
    public void addEventListener(Conversation conversation, MessageEventListener listener) {
        if (null == this.conversationMessageListeners) {
            this.conversationMessageListeners = new HashMap<>();
        }

        List<MessageEventListener> list = this.conversationMessageListeners.get(conversation.id);
        if (null == list) {
            list = new Vector<>();
            list.add(listener);
            this.conversationMessageListeners.put(conversation.id, list);
        }
        else {
            list.add(listener);
        }
    }

    /**
     * 移除指定会话的消息事件监听器。
     *
     * @param conversation 指定会话。
     * @param listener 消息事件监听器。
     */
    public void removeEventListener(Conversation conversation, MessageEventListener listener) {
        if (null == this.conversationMessageListeners) {
            return;
        }

        List<MessageEventListener> list = this.conversationMessageListeners.get(conversation.id);
        if (null != list) {
            list.remove(listener);
        }
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
     * 获取最近的会话清单。
     *
     * @return 返回消息列表。如果返回 {@code null} 值表示消息服务模块未启动。
     */
    public List<Conversation> getRecentConversations() {
        return this.getRecentConversations(20);
    }

    /**
     * 获取最近的会话清单。
     *
     * @param maxLimit 指定最大记录数量。
     * @return 返回消息列表。如果返回 {@code null} 值表示消息服务模块未启动。
     */
    public List<Conversation> getRecentConversations(int maxLimit) {
        if (!this.hasStarted()) {
            return null;
        }

        if (!this.ready) {
            synchronized (this.preparing) {
                try {
                    this.preparing.wait(5000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        synchronized (this) {
            if (null == this.conversations || this.conversations.isEmpty()) {
                List<Conversation> list = this.storage.queryRecentConversations(maxLimit);

                if (null == this.conversations) {
                    this.conversations = new Vector<>(list);
                }
                else {
                    this.conversations.addAll(list);
                }

                this.sortConversationList(this.conversations);
            }
        }

        return this.conversations;
    }

    /**
     * 获取指定 ID 的会话。
     *
     * @param id 指定会话 ID 。
     * @return 返回会话实例。
     */
    public Conversation getConversation(Long id) {
        synchronized (this) {
            if (null == this.conversations || this.conversations.isEmpty()) {
                return this.storage.readConversation(id);
            }
            else {
                for (Conversation conversation : this.conversations) {
                    if (conversation.id.longValue() == id.longValue()) {
                        return conversation;
                    }
                }
            }
        }

        // 从存储里读取
        return this.storage.readConversation(id);
    }

    /**
     * 标记指定会话里的所有消息为已读。
     *
     * @param conversation
     */
    public void markRead(Conversation conversation) {
        
    }

    /**
     * 获取会话的最近消息。
     *
     * @param conversation
     * @param limit
     * @return
     */
    public MessageListResult getRecentMessages(Conversation conversation, int limit) {
        MessageList list = this.conversationMessageListMap.get(conversation.id);
        if (null != list) {
            if (!list.messages.isEmpty()) {
                // 延迟实体寿命
                list.toExtendLife(3L * 60L * 1000L);

                final List<Message> resultList = new ArrayList<>(list.messages);
                final boolean hasMore = list.hasMore;

                return new MessageListResult() {
                    @Override
                    public List<Message> getList() {
                        return resultList;
                    }

                    @Override
                    public boolean hasMore() {
                        return hasMore;
                    }
                };
            }
        }
        else {
            list = new MessageList();
            this.conversationMessageListMap.put(conversation.id, list);
            // 托管列表内的实体生命周期
            this.kernel.getInspector().depositList(list.messages);
        }

        if (conversation.getType() == ConversationType.Contact) {
            MessageListResult result = this.storage.queryMessagesByReverseWithContact(conversation.getContact().getId(),
                    System.currentTimeMillis(), limit);
            // 从数据库里查出来的是时间倒序，从大到小
            // 这里对列表进行翻转，翻转为时间正序
            Collections.reverse(result.getList());

            // 重置列表
            list.reset(result);

            return result;
        }

        return null;
    }

    /**
     * 查询指定联系人的消息。
     *
     * @param contact
     * @param timestamp
     * @param limit
     * @return
     */
    public MessageListResult queryMessages(Contact contact, long timestamp, int limit) {

        return null;
    }

    private void prepare(CompletionHandler handler) {
        this.preparing.set(true);

        Self self = this.contactService.getSelf();

        // 开启存储
        this.storage.open(this.getContext(), self.id, self.domain);

        long now = System.currentTimeMillis();

        MutableBoolean first = new MutableBoolean(false);

        // 查询本地最近消息时间
        long time = this.storage.queryLastMessageTime();
        if (0 == time) {
            first.value = true;
            this.lastMessageTime = now - this.retrospectDuration;
        }
        else {
            this.lastMessageTime = time;
        }

        // 服务就绪
        this.ready = true;

        // 进行线程通知
        synchronized (this.preparing) {
            this.preparing.notifyAll();
        }

        if (!first.value) {
            // 不是第一次获取数据或者未能连接到服务器，直接回调
            this.execute(new Runnable() {
                @Override
                public void run() {
                    handler.handleCompletion(MessagingService.this);
                }
            });
        }

        MutableBoolean gotMessages = new MutableBoolean(false);
        MutableBoolean gotConversations = new MutableBoolean(false);

        // 从服务器上拉取自上一次时间戳之后的所有消息
        this.queryRemoteMessage(this.lastMessageTime + 1, now, new CompletionHandler() {
            @Override
            public void handleCompletion(Module module) {
                gotMessages.value = true;
                if (gotConversations.value) {
                    preparing.set(false);
                    if (first.value) {
                        handler.handleCompletion(MessagingService.this);
                    }
                }
            }
        });

        // 获取最新的会话列表
        // 根据最近一次消息时间戳更新数量
        int limit = 30;
        if (now - this.lastMessageTime > 1L * 60L * 60L * 1000L) {
            // 大于一小时
            limit = 50;
        }
        this.queryRemoteConversations(limit, new CompletionHandler() {
            @Override
            public void handleCompletion(Module module) {
                gotConversations.value = true;
                if (gotMessages.value) {
                    preparing.set(false);
                    if (first.value) {
                        handler.handleCompletion(MessagingService.this);
                    }
                }
            }
        });
    }

    private void queryRemoteMessage(long beginning, long ending, CompletionHandler completionHandler) {
        if (!this.pipeline.isReady() && this.isAvailableNetwork()) {
            synchronized (this) {
                try {
                    this.wait(5000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

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

    private void queryRemoteConversations(int limit, CompletionHandler completionHandler) {
        if (!this.pipeline.isReady() && this.isAvailableNetwork()) {
            synchronized (this) {
                try {
                    this.wait(5000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!this.pipeline.isReady()) {
            completionHandler.handleCompletion(null);
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("limit", limit);
        } catch (JSONException e) {
            // Nothing
        }
        Packet requestPacket = new Packet(MessagingAction.GetConversations, payload);
        this.pipeline.send(MessagingService.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    completionHandler.handleCompletion(null);
                    return;
                }

                if (packet.extractServiceStateCode() != MessagingServiceState.Ok.code) {
                    completionHandler.handleCompletion(null);
                    return;
                }

                List<Conversation> conversationList = new ArrayList<>();
                JSONObject data = packet.extractServiceData();
                try {
                    // 读取列表
                    JSONArray list = data.getJSONArray("list");
                    for (int i = 0; i < list.length(); ++i) {
                        Conversation conversation = new Conversation(list.getJSONObject(i));
                        fillConversation(conversation);
                        conversationList.add(conversation);
                    }
                } catch (JSONException e) {
                    Log.d(MessagingService.class.getSimpleName(),
                            "#queryRemoteConversations : error conversation list format");
                }

                execute(new Runnable() {
                    @Override
                    public void run() {
                        if (conversationList.isEmpty()) {
                            // 回调
                            completionHandler.handleCompletion(MessagingService.this);
                            return;
                        }

                        boolean changed = true;
                        Conversation lastConversation = storage.getLastConversation();
                        Conversation firstConversation = conversationList.get(0);
                        if (null != lastConversation && lastConversation.equals(firstConversation)) {
                            // 本地存储的最近会话与服务器返回的一致，数据没有变化
                            // 这样判断会有误判，但是仅需要比较一条记录
                            changed = false;
                        }

                        // 更新到数据库
                        storage.updateConversations(conversationList);

                        // 回调
                        completionHandler.handleCompletion(MessagingService.this);

                        // 回调事件监听器
                        if (changed && null != recentEventListener) {
                            synchronized (MessagingService.this) {
                                if (null != conversations) {
                                    conversations.clear();
                                }
                            }

                            recentEventListener.onConversationListUpdated(getRecentConversations(), MessagingService.this);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void notifyObservers(ObservableEvent event) {
        super.notifyObservers(event);

        if (MessagingServiceEvent.Notify.equals(event.getName())) {
            Message message = (Message) event.getData();

            if (null != this.eventListeners) {
                for (MessageEventListener listener : this.eventListeners) {
                    listener.onMessageReceived(message, this);
                }
            }

            if (null != this.conversationMessageListeners) {
                Long convId = message.isFromGroup() ? message.getSource() : message.getPartnerId();
                List<MessageEventListener> list = this.conversationMessageListeners.get(convId);
                if (null != list) {
                    for (MessageEventListener listener : list) {
                        listener.onMessageReceived(message, this);
                    }
                }
            }
        }
    }

    private void appendMessageToConversation(Long conversationId, Message message) {
        if (null != this.conversations && !this.conversations.isEmpty()) {
            for (Conversation conversation : this.conversations) {
                if (conversation.getPivotalId().longValue() == conversationId) {
                    conversation.setRecentMessage(message);
                    this.sortConversationList(this.conversations);
                    break;
                }
            }
        }

        MessageList list = this.conversationMessageListMap.get(conversationId);
        if (null != list) {
            list.messages.add(message);
        }

        // 更新会话数据库
        this.storage.updateRecentMessage(conversationId, message);
    }

    protected void fireContactEvent(ObservableEvent event) {
        if (ContactServiceEvent.SelfReady.equals(event.name)) {
            synchronized (this) {
                if (!this.ready && !this.preparing.get()) {
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
        else if (ContactServiceEvent.SignOut.equals(event.name)) {
            // TODO
        }
    }

    protected void triggerNotify(JSONObject data) {
        Message message = null;
        try {
            message = new Message(this, data);
        } catch (JSONException e) {
            Log.w("MessagingService", "#triggerNotify", e);
        }

        // 填充消息相关实体对象
        Message compMessage = this.fillMessage(message);

        // 数据写入数据库
        boolean exists = this.storage.updateMessage(message);

        if (message.getRemoteTimestamp() > this.lastMessageTime) {
            this.lastMessageTime = message.getRemoteTimestamp();
        }

        if (!exists) {
            // 更新会话清单
            long pivotalId = compMessage.isFromGroup() ? compMessage.getSource() : compMessage.getPartnerId();
            this.appendMessageToConversation(pivotalId, compMessage);

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

    protected Message fillMessage(Message message) {
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

        // 实例化
        Hook<Message> hook = (Hook<Message>) this.pluginSystem.getHook(InstantiateHook.NAME);
        Message compMessage = hook.apply(message);
        return compMessage;
    }

    protected void fillConversation(Conversation conversation) {
        // 填充消息
        Message recentMessage = this.fillMessage(conversation.getRecentMessage());
        conversation.setRecentMessage(recentMessage);

        if (ConversationType.Contact == conversation.getType()) {
            Contact contact = this.contactService.getContact(conversation.getPivotalId());
            conversation.setPivotal(contact);
        }
        else if (ConversationType.Group == conversation.getType()) {
            // TODO
        }
    }

    private void sortConversationList(List<Conversation> list) {
        // 将 Important 置顶
        Collections.sort(list, new Comparator<Conversation>() {
            @Override
            public int compare(Conversation c1, Conversation c2) {
                if (c1.getState().code == c2.getState().code) {
                    // 相同状态，判断时间戳
                    if (c1.getTimestamp() < c2.getTimestamp()) {
                        return 1;
                    }
                    else if (c1.getTimestamp() > c2.getTimestamp()) {
                        return -1;
                    }
                    else {
                        return 0;
                    }
                }
                else {
                    if (c1.getState() == ConversationState.Normal && c2.getState() == ConversationState.Important) {
                        // 交换顺序，把 c2 排到前面
                        return 1;
                    }
                    else if (c1.getState() == ConversationState.Important && c2.getState() == ConversationState.Normal) {
                        return -1;
                    }
                }

                return 0;
            }
        });
    }
}
