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

package cube.contact;

import android.util.Log;
import android.util.MutableBoolean;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import cube.auth.AuthToken;
import cube.contact.handler.ContactAppendixHandler;
import cube.contact.handler.ContactHandler;
import cube.contact.handler.ContactListHandler;
import cube.contact.handler.GroupListHandler;
import cube.contact.handler.SignHandler;
import cube.contact.handler.TopListHandler;
import cube.contact.model.AbstractContact;
import cube.contact.model.Contact;
import cube.contact.model.ContactAppendix;
import cube.contact.model.Group;
import cube.contact.model.MutableContact;
import cube.contact.model.Self;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.Packet;
import cube.core.PipelineState;
import cube.core.handler.FailureHandler;
import cube.core.handler.PipelineHandler;
import cube.core.model.TimeSortable;
import cube.util.ObservableEvent;

/**
 * 联系人模块。
 */
public class ContactService extends Module {

    /**
     * 模块名。
     */
    public final static String NAME = "Contact";

    /** 阻塞调用方法的超时时间。 */
    private final long blockingTimeout = 3000L;

    private long retrospectDuration = 30L * 24L * 60L * 60000L;

    private ContactStorage storage;

    private ContactPipelineListener pipelineListener;

    protected AtomicBoolean selfReady;

    private SignHandler signInHandler;

    protected Self self;

    private ContactDataProvider contactDataProvider;

    private ConcurrentHashMap<Long, AbstractContact> cache;

    public ContactService() {
        super(ContactService.NAME);
        this.selfReady = new AtomicBoolean(false);
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }

        this.storage = new ContactStorage(this);

        this.pipelineListener = new ContactPipelineListener(this);
        this.pipeline.addListener(NAME, this.pipelineListener);

        this.kernel.getInspector().depositMap(this.cache);

        return true;
    }

    @Override
    public void stop() {
        super.stop();

        this.kernel.getInspector().withdrawMap(this.cache);
        this.cache.clear();

        this.signInHandler = null;

        if (null != this.pipelineListener) {
            this.pipeline.removeListener(NAME, this.pipelineListener);
            this.pipelineListener = null;
        }

        if (null != this.storage) {
            this.storage.close();
            this.storage = null;
        }
    }

    @Override
    public boolean isReady() {
        return this.selfReady.get() && (null != this.self);
    }

    /**
     * 设置联系人数据提供者。
     *
     * @param provider 指定数据提供者。
     */
    public void setContactDataProvider(ContactDataProvider provider) {
        this.contactDataProvider = provider;
    }

    /**
     * 签入指定联系人。
     *
     * @param self
     * @param handler 该操作的回调句柄。
     * @return 如果返回 {@code false} 表示当前状态下不能进行该操作，请检查是否正确启动魔方。
     */
    public boolean signIn(Self self, SignHandler handler) {
        if (null != this.self && !this.self.equals(self)) {
            Log.w("ContactService", "Can NOT use different contact to sign-in");
            return false;
        }

        if (this.selfReady.get()) {
            return false;
        }

        if (!this.hasStarted()) {
            if (!this.start()) {
                // 启动模块失败
                return false;
            }
        }

        // 等待内核就绪
        int count = 100;
        while (!this.kernel.isReady() && count > 0) {
            --count;
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                // Nothing
            }
        }

        if (!this.kernel.isReady()) {
            // 内核未就绪
            return false;
        }

        // 开启存储
        this.storage.open(this.getContext(), self.id, self.domain);

        // 设置 Self
        this.self = self;

        if (!this.pipeline.isReady()) {
            // 数据通道未就绪从数据库读取数据
            Contact contact = this.storage.readContact(self.id);
            // 激活令牌
            AuthToken authToken = this.kernel.activeToken(self.id);

            if (null != contact && null != authToken) {
                // 设置附录
                this.self.setAppendix(contact.getAppendix());
                // 设置上下文
                if (null != this.self.getContext()) {
                    // 更新联系人的上下文
                    long last = this.storage.updateContactContext(this.self.id, this.self.getContext());
                    this.self.resetLast(last);
                }
                else {
                    this.self.setContext(contact.getContext());
                }

                this.execute(new Runnable() {
                    @Override
                    public void run() {
                        ObservableEvent event = new ObservableEvent(ContactServiceEvent.SelfReady, ContactService.this.self);
                        notifyObservers(event);

                        if (null != handler) {
                            handler.handleSuccess(ContactService.this, ContactService.this.self);
                        }
                    }
                });
            }
            else {
                if (null != handler) {
                    this.execute(new Runnable() {
                        @Override
                        public void run() {
                            handler.handleFailure(ContactService.this,
                                    new ModuleError(ContactService.NAME, ContactServiceState.NoNetwork.code));
                        }
                    });
                }
            }

            return true;
        }

        // 访问服务器进行签入

        // 通知系统 Self 实例就绪
        this.execute(new Runnable() {
            @Override
            public void run() {
                ObservableEvent event = new ObservableEvent(ContactServiceEvent.SelfReady, ContactService.this.self);
                notifyObservers(event);
            }
        });

        // 激活令牌
        AuthToken authToken = this.kernel.activeToken(self.id);
        if (null != authToken) {
            if (null != handler) {
                // 设置回调
                this.signInHandler = handler;
            }

            // 请求服务器进行签入
            JSONObject payload = new JSONObject();
            try {
                payload.put("self", this.self.toJSON());
                payload.put("token", authToken.toJSON());
            } catch (JSONException e) {
                Log.e(ContactService.class.getSimpleName(), "#signIn", e);
            }
            Packet signInPacket = new Packet(ContactServiceAction.SignIn, payload);
            this.pipeline.send(ContactService.NAME, signInPacket);
        }
        else {
            if (null != handler) {
                this.execute(new Runnable() {
                    @Override
                    public void run() {
                        handler.handleFailure(ContactService.this,
                                new ModuleError(ContactService.NAME, ContactServiceState.InconsistentToken.code));
                    }
                });
            }
        }

        return true;
    }

    /**
     * 当前联系人签出。
     *
     * @param handler 该操作的回调句柄。
     * @retrun 如果返回 {@code false} 表示当前状态下不能进行该操作。
     */
    public boolean signOut(SignHandler handler) {
        if (!this.selfReady.get()) {
            return false;
        }

        if (!this.pipeline.isReady()) {
            // 无网络状态下签出
            this.selfReady.set(false);

            // 关闭存储
            this.storage.close();

            this.execute(new Runnable() {
                @Override
                public void run() {
                    ObservableEvent event = new ObservableEvent(ContactServiceEvent.SignOut, self);
                    notifyObservers(event);

                    handler.handleSuccess(ContactService.this, self);
                    self = null;
                }
            });

            return true;
        }

        Packet signOutPacket = new Packet(ContactServiceAction.SignOut, this.self.toJSON());
        this.pipeline.send(ContactService.NAME, signOutPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            ModuleError error = new ModuleError(ContactService.NAME, packet.state.code);
                            handler.handleFailure(ContactService.this, error);
                        }
                    });
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            ModuleError error = new ModuleError(ContactService.NAME, stateCode);
                            handler.handleFailure(ContactService.this, error);
                        }
                    });
                    return;
                }

                selfReady.set(false);

                // 关闭存储器
                storage.close();

                final Self current = self;
                execute(new Runnable() {
                    @Override
                    public void run() {
                        handler.handleSuccess(ContactService.this, current);
                    }
                });
            }
        });

        return true;
    }

    /**
     * 获取当前签入的联系人。
     *
     * @return 返回当前签入的联系人。
     */
    public Self getSelf() {
        return this.self;
    }

    /**
     * 获取指定 ID 的联系人。
     *
     * <b>不建议在主线程里调用该方法。</b>
     *
     * @param contactId 指定联系人 ID 。
     * @return 返回联系人实例。如果没有获取到数据返回 {code null} 值。
     */
    public Contact getContact(Long contactId) {
        final MutableContact mutableContact = new MutableContact();

        this.getContact(contactId, new ContactHandler() {
            @Override
            public void handleContact(Contact contact) {
                // 赋值
                mutableContact.contact = contact;

                synchronized (mutableContact) {
                    mutableContact.notify();
                }
            }
        }, new FailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                synchronized (mutableContact) {
                    mutableContact.notify();
                }
            }
        });

        synchronized (mutableContact) {
            try {
                mutableContact.wait(this.blockingTimeout);
            } catch (InterruptedException e) {
                // Nothing
            }
        }

        return mutableContact.contact;
    }

    /**
     * 获取指定 ID 的联系人。
     *
     * @param contactId 指定联系人 ID 。
     * @param successHandler 成功获取到数据回调该句柄。
     */
    public void getContact(Long contactId, ContactHandler successHandler) {
        this.getContact(contactId, successHandler, null);
    }

    /**
     * 获取指定 ID 的联系人。
     *
     * @param contactId 指定联系人 ID 。
     * @param successHandler 成功获取到数据回调该句柄。
     * @param failureHandler 获取数据时故障回调该句柄。该句柄可以设置为 {@code null} 值。
     */
    public void getContact(Long contactId, ContactHandler successHandler, FailureHandler failureHandler) {
        if (!this.selfReady.get()) {
            if (null != failureHandler) {
                this.execute(new Runnable() {
                    @Override
                    public void run() {
                        ModuleError error = new ModuleError(ContactService.NAME, ContactServiceState.IllegalOperation.code);
                        failureHandler.handleFailure(ContactService.this, error);
                    }
                });
            }
            return;
        }

        if (contactId.longValue() == this.self.id.longValue()) {
            this.execute(new Runnable() {
                @Override
                public void run() {
                    successHandler.handleContact(self);
                }
            });
            return;
        }

        // 从缓存里读取
        AbstractContact abstractContact = this.cache.get(contactId);
        if (null != abstractContact && (abstractContact instanceof Contact)) {
            this.execute(new Runnable() {
                @Override
                public void run() {
                    successHandler.handleContact((Contact) abstractContact);
                }
            });
            return;
        }

        // 从数据库读取
        Contact contact = this.storage.readContact(contactId);
        if (null != contact && contact.isValid()) {
            // 有效的数据
            // 检查上下文
            if (null == contact.getContext() && null != this.contactDataProvider) {
                contact.setContext(this.contactDataProvider.needContactContext(contact));
                if (null != contact.getContext()) {
                    long last = this.storage.updateContactContext(contact.id, contact.getContext());
                    contact.resetLast(last);
                }
            }

            // 写入缓存
            this.cache.put(contactId, contact);

            this.execute(new Runnable() {
                @Override
                public void run() {
                    successHandler.handleContact(contact);
                }
            });
            return;
        }

        if (!this.pipeline.isReady()) {
            // 数据通道未就绪
            if (null != failureHandler) {
                this.execute(new Runnable() {
                    @Override
                    public void run() {
                        ModuleError error = new ModuleError(NAME, ContactServiceState.NoNetwork.code);
                        failureHandler.handleFailure(ContactService.this, error);
                    }
                });
            }
            return;
        }

        JSONObject packetData = new JSONObject();
        try {
            packetData.put("id", contactId.longValue());
            packetData.put("domain", this.getAuthToken().domain);
        } catch (JSONException e) {
            Log.w(ContactService.class.getName(), "#getContact", e);
        }

        Packet requestPacket = new Packet(ContactServiceAction.GetContact, packetData);
        this.pipeline.send(ContactService.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    if (null != failureHandler) {
                        execute(new Runnable() {
                            @Override
                            public void run() {
                                ModuleError error = new ModuleError(ContactService.NAME, ContactServiceState.ServerError.code);
                                failureHandler.handleFailure(ContactService.this, error);
                            }
                        });
                    }
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    if (null != failureHandler) {
                        execute(new Runnable() {
                            @Override
                            public void run() {
                                ModuleError error = new ModuleError(ContactService.NAME, stateCode);
                                failureHandler.handleFailure(ContactService.this, error);
                            }
                        });
                    }
                    return;
                }

                try {
                    JSONObject json = packet.extractServiceData();
                    json.put("domain", getAuthToken().domain);
                    Contact contact = new Contact(json);

                    if (null == contact.getContext() && null != contactDataProvider) {
                        contact.setContext(contactDataProvider.needContactContext(contact));
                    }

                    // 写入数据库
                    storage.writeContact(contact);

                    // 获取附录
                    getAppendix(contact, new ContactAppendixHandler() {
                        @Override
                        public void handleAppendix(Contact contact, ContactAppendix appendix) {
                            successHandler.handleContact(contact);
                        }
                    }, new FailureHandler() {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            if (null != failureHandler) {
                                failureHandler.handleFailure(module, error);
                            }
                        }
                    });

                    // 写入缓存
                    cache.put(contactId, contact);
                } catch (JSONException e) {
                    // Nothing
                }
            }
        });
    }

    protected void getAppendix(Contact contact, ContactAppendixHandler successHandler, FailureHandler failureHandler) {
        JSONObject data = new JSONObject();
        try {
            data.put("contactId", contact.id.longValue());
        } catch (JSONException e) {
            // Nothing
        }
        Packet request = new Packet(ContactServiceAction.GetAppendix, data);
        this.pipeline.send(ContactService.NAME, request, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            ModuleError error = new ModuleError(ContactService.NAME, packet.state.code);
                            failureHandler.handleFailure(ContactService.this, error);
                        }
                    });
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            ModuleError error = new ModuleError(ContactService.NAME, stateCode);
                            failureHandler.handleFailure(ContactService.this, error);
                        }
                    });
                    return;
                }

                try {
                    ContactAppendix appendix = new ContactAppendix(ContactService.this, contact,
                            packet.extractServiceData());
                    // 更新存储
                    storage.writeAppendix(appendix);

                    // 赋值
                    contact.setAppendix(appendix);

                    execute(new Runnable() {
                        @Override
                        public void run() {
                            successHandler.handleAppendix(contact, appendix);
                        }
                    });
                }
                catch (JSONException e) {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            ModuleError error = new ModuleError(ContactService.NAME, ContactServiceState.DataStructureError.code);
                            failureHandler.handleFailure(ContactService.this, error);
                        }
                    });
                }
            }
        });
    }

    private void listGroups(long beginning, long ending, GroupListHandler handler) {
        // TODO
        ArrayList<Group> list = new ArrayList<>(1);
        this.execute(new Runnable() {
            @Override
            public void run() {
                handler.handleList(list);
            }
        });
    }

    private void listBlockList(ContactListHandler handler) {
        // TODO
        ArrayList<Contact> list = new ArrayList<>(1);
        this.execute(new Runnable() {
            @Override
            public void run() {
                handler.handleList(list);
            }
        });
    }

    private void listTopList(TopListHandler handler) {
        // TODO
        ArrayList<TimeSortable> list = new ArrayList<>(1);
        this.execute(new Runnable() {
            @Override
            public void run() {
                handler.handleList(list);
            }
        });
    }

    private void fireSignInCompleted() {
        Log.d("ContactService", "#fireSignInCompleted");

        if (this.selfReady.get()) {
            return;
        }

        this.selfReady.set(true);

        // 写入数据库
        this.storage.writeContact(this.self);

        this.execute(new Runnable() {
            @Override
            public void run() {
                // 通知 Sign-In 事件
                ObservableEvent event = new ObservableEvent(ContactServiceEvent.SignIn, self);
                notifyObservers(event);

                if (null != ContactService.this.signInHandler) {
                    ContactService.this.signInHandler.handleSuccess(ContactService.this,
                            ContactService.this.self);
                    ContactService.this.signInHandler = null;
                }
            }
        });
    }

    protected void triggerSignIn(int stateCode, JSONObject payload) {
        if (stateCode != ContactServiceState.Ok.code) {
            ObservableEvent event = new ObservableEvent(ContactServiceEvent.Fault, stateCode);
            this.notifyObservers(event);
            return;
        }

        try {
            if (null != this.self) {
                this.self.update(payload);
            }
            else {
                this.self = new Self(payload);
            }
        } catch (JSONException e) {
            Log.e(ContactService.class.getSimpleName(), "#triggerSignIn", e);
        }

        MutableBoolean gotAppendix = new MutableBoolean(false);
        MutableBoolean gotGroups = new MutableBoolean(false);
        MutableBoolean gotBlockList = new MutableBoolean(false);
        MutableBoolean gotTopList = new MutableBoolean(false);

        SignInCompletion completion = () -> {
            if (gotAppendix.value && gotGroups.value && gotBlockList.value && gotTopList.value) {
                fireSignInCompleted();
            }
        };

        long now = System.currentTimeMillis();

        // 获取附录
        this.getAppendix(this.self, new ContactAppendixHandler() {
            @Override
            public void handleAppendix(Contact contact, ContactAppendix appendix) {
                gotAppendix.value = true;
                completion.check();
            }
        }, new FailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                gotAppendix.value = true;
                completion.check();
            }
        });

        // 更新群组列表
        this.listGroups(now - this.retrospectDuration, now, new GroupListHandler() {
            @Override
            public void handleList(List<Group> groupList) {
                gotGroups.value = true;
                completion.check();
            }
        });

        // 更新阻止清单
        this.listBlockList(new ContactListHandler() {
            @Override
            public void handleList(List<Contact> contactList) {
                gotBlockList.value = true;
                completion.check();
            }
        });

        // 更新置顶清单
        this.listTopList(new TopListHandler() {
            @Override
            public void handleList(List<TimeSortable> list) {
                gotTopList.value = true;
                completion.check();
            }
        });
    }

    protected void triggerSignOut() {
        ObservableEvent event = new ObservableEvent(ContactServiceEvent.SignOut, this.self);
        this.notifyObservers(event);

        this.self = null;
    }

    interface SignInCompletion {
        void check();
    }
}
