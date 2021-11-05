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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import cube.auth.AuthToken;
import cube.contact.handler.ContactAppendixHandler;
import cube.contact.handler.ContactHandler;
import cube.contact.handler.ContactListHandler;
import cube.contact.handler.ContactZoneHandler;
import cube.contact.handler.ContactZoneListHandler;
import cube.contact.handler.DefaultContactAppendixHandler;
import cube.contact.handler.DefaultContactHandler;
import cube.contact.handler.DefaultContactZoneHandler;
import cube.contact.handler.GroupListHandler;
import cube.contact.handler.SignHandler;
import cube.contact.handler.StableContactZoneHandler;
import cube.contact.model.AbstractContact;
import cube.contact.model.Contact;
import cube.contact.model.ContactAppendix;
import cube.contact.model.ContactZone;
import cube.contact.model.ContactZoneParticipant;
import cube.contact.model.ContactZoneState;
import cube.contact.model.Group;
import cube.contact.model.MutableContact;
import cube.contact.model.MutableContactZone;
import cube.contact.model.Self;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.Packet;
import cube.core.PipelineState;
import cube.core.handler.CompletionHandler;
import cube.core.handler.DefaultFailureHandler;
import cube.core.handler.FailureHandler;
import cube.core.handler.PipelineHandler;
import cube.core.handler.StableFailureHandler;
import cube.util.LogUtils;
import cube.util.ObservableEvent;

/**
 * 联系人模块。
 */
public class ContactService extends Module {

    /**
     * 模块名。
     */
    public final static String NAME = "Contact";

    /**
     * 内存寿命。
     */
    private final static long LIFESPAN = 5L * 60L * 1000L;

    /** 阻塞调用方法的超时时间。 */
    private final long blockingTimeout = 3000L;

    /** 默认提供的联系人分区名称，签入时会自动加载该分区。 */
    private final String defaultContactZoneName = "contacts";

    private long retrospectDuration = 30L * 24L * 60L * 60000L;

    private ContactStorage storage;

    private ContactPipelineListener pipelineListener;

    protected AtomicBoolean signInReady;

    private SignHandler signInHandler;

    protected Self self;

    private ContactZone defaultContactZone;

    private ContactDataProvider contactDataProvider;

    private ConcurrentHashMap<Long, AbstractContact> cache;

    private List<ContactZoneListener> contactZoneListenerList;

    public ContactService() {
        super(ContactService.NAME);
        this.signInReady = new AtomicBoolean(false);
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

        this.signInReady.set(false);
    }

    @Override
    public boolean isReady() {
        return this.signInReady.get() && (null != this.self);
    }

    @Override
    protected void execute(Runnable runnable) {
        super.execute(runnable);
    }

    /**
     * 设置联系人数据提供者。
     *
     * @param provider 指定数据提供者。
     */
    public void setContactDataProvider(ContactDataProvider provider) {
        this.contactDataProvider = provider;
    }

    public void addContactZoneListener(ContactZoneListener listener) {
        if (null == this.contactZoneListenerList) {
            this.contactZoneListenerList = new ArrayList<>();
        }

        if (!this.contactZoneListenerList.contains(listener)) {
            this.contactZoneListenerList.add(listener);
        }
    }

    public void removeContactZoneListener(ContactZoneListener listener) {
        if (null != this.contactZoneListenerList) {
            this.contactZoneListenerList.remove(listener);
        }
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

        if (this.signInReady.get()) {
            return false;
        }

        if (!this.hasStarted()) {
            if (!this.start()) {
                // 启动模块失败
                return false;
            }
        }

        // 等待内核就绪
        int count = 500;
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
                    long last = this.storage.updateContactContext(this.self);
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
                            executeOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    handler.handleSuccess(ContactService.this, ContactService.this.self);
                                }
                            });
                        }
                    }
                });
            }
            else {
                if (null != handler) {
                    this.executeOnMainThread(new Runnable() {
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

        // 激活令牌
        AuthToken authToken = this.kernel.activeToken(self.id);
        if (null != authToken) {
            // 通知系统 Self 实例就绪
            this.execute(new Runnable() {
                @Override
                public void run() {
                    ObservableEvent event = new ObservableEvent(ContactServiceEvent.SelfReady, ContactService.this.self);
                    notifyObservers(event);
                }
            });

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
                this.executeOnMainThread(new Runnable() {
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
        if (null == this.self) {
            return false;
        }

        if (!this.pipeline.isReady()) {
            // 无网络状态下签出
            this.signInReady.set(false);

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

                signInReady.set(false);

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
        if (null == this.self) {
            return null;
        }

        final MutableContact mutableContact = new MutableContact();

        this.getContact(contactId, new DefaultContactHandler() {
            @Override
            public void handleContact(Contact contact) {
                // 赋值
                mutableContact.contact = contact;

                synchronized (mutableContact) {
                    mutableContact.notify();
                }
            }
        }, new DefaultFailureHandler() {
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
        if (null == this.self) {
            if (null != failureHandler) {
                Runnable callback = new Runnable() {
                    @Override
                    public void run() {
                        ModuleError error = new ModuleError(ContactService.NAME, ContactServiceState.IllegalOperation.code);
                        failureHandler.handleFailure(ContactService.this, error);
                    }
                };

                if (failureHandler.isInMainThread()) {
                    this.executeOnMainThread(callback);
                }
                else {
                    this.execute(callback);
                }
            }
            return;
        }

        if (contactId.longValue() == this.self.id.longValue()) {
            if (successHandler.isInMainThread()) {
                this.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        successHandler.handleContact(self);
                    }
                });
            }
            else {
                this.execute(new Runnable() {
                    @Override
                    public void run() {
                        successHandler.handleContact(self);
                    }
                });
            }

            return;
        }

        // 从缓存里读取
        AbstractContact abstractContact = this.cache.get(contactId);
        if (null != abstractContact) {
            // 更新缓存寿命
            abstractContact.entityLifeExpiry += LIFESPAN;

            if (successHandler.isInMainThread()) {
                this.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        successHandler.handleContact((Contact) abstractContact);
                    }
                });
            }
            else {
                this.execute(new Runnable() {
                    @Override
                    public void run() {
                        successHandler.handleContact((Contact) abstractContact);
                    }
                });
            }

            return;
        }

        // 从数据库读取
        Contact contact = this.storage.readContact(contactId);
        if (null != contact) {
            // 在没有连接服务器或者数据有效时返回
            if (!this.pipeline.isReady() || contact.isValid()) {
                // 检查上下文
                if (null == contact.getContext() && null != this.contactDataProvider) {
                    contact.setContext(this.contactDataProvider.needContactContext(contact));
                    if (null != contact.getContext()) {
                        long last = this.storage.updateContactContext(contact);
                        contact.resetLast(last);
                    }
                    this.storage.updateContactName(contact);
                }

                // 写入缓存
                this.cache.put(contactId, contact);

                if (successHandler.isInMainThread()) {
                    this.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            successHandler.handleContact(contact);
                        }
                    });
                }
                else {
                    this.execute(new Runnable() {
                        @Override
                        public void run() {
                            successHandler.handleContact(contact);
                        }
                    });
                }

                return;
            }
        }

        if (!this.pipeline.isReady()) {
            // 数据通道未就绪
            if (null != failureHandler) {
                if (failureHandler.isInMainThread()) {
                    this.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            ModuleError error = new ModuleError(NAME, ContactServiceState.NoNetwork.code);
                            failureHandler.handleFailure(ContactService.this, error);
                        }
                    });
                }
                else {
                    this.execute(new Runnable() {
                        @Override
                        public void run() {
                            ModuleError error = new ModuleError(NAME, ContactServiceState.NoNetwork.code);
                            failureHandler.handleFailure(ContactService.this, error);
                        }
                    });
                }
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
                        if (failureHandler.isInMainThread()) {
                            executeOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    ModuleError error = new ModuleError(ContactService.NAME, ContactServiceState.ServerError.code);
                                    failureHandler.handleFailure(ContactService.this, error);
                                }
                            });
                        }
                        else {
                            execute(new Runnable() {
                                @Override
                                public void run() {
                                    ModuleError error = new ModuleError(ContactService.NAME, ContactServiceState.ServerError.code);
                                    failureHandler.handleFailure(ContactService.this, error);
                                }
                            });
                        }
                    }

                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    if (null != failureHandler) {
                        if (failureHandler.isInMainThread()) {
                            executeOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    ModuleError error = new ModuleError(ContactService.NAME, stateCode);
                                    failureHandler.handleFailure(ContactService.this, error);
                                }
                            });
                        }
                        else {
                            execute(new Runnable() {
                                @Override
                                public void run() {
                                    ModuleError error = new ModuleError(ContactService.NAME, stateCode);
                                    failureHandler.handleFailure(ContactService.this, error);
                                }
                            });
                        }
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
                    getAppendix(contact, new DefaultContactAppendixHandler() {
                        @Override
                        public void handleAppendix(Contact contact, ContactAppendix appendix) {
                            // 写入缓存
                            cache.put(contactId, contact);

                            if (successHandler.isInMainThread()) {
                                executeOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        successHandler.handleContact(contact);
                                    }
                                });
                            }
                            else {
                                successHandler.handleContact(contact);
                            }
                        }
                    }, new DefaultFailureHandler((null != failureHandler) && failureHandler.isInMainThread()) {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            if (null != failureHandler) {
                                failureHandler.handleFailure(module, error);
                            }
                        }
                    });
                } catch (JSONException e) {
                    LogUtils.w(ContactService.class.getSimpleName(), e);
                }
            }
        });
    }

    /**
     * 获取默认的联系人分区。
     *
     * @return
     */
    public ContactZone getDefaultContactZone() {
        if (null != this.defaultContactZone) {
            return this.defaultContactZone;
        }

        MutableContactZone zone = new MutableContactZone();

        this.getContactZone(this.defaultContactZoneName, new DefaultContactZoneHandler() {
            @Override
            public void handleContactZone(ContactZone contactZone) {
                zone.contactZone = contactZone;

                synchronized (zone) {
                    zone.notify();
                }
            }
        }, new DefaultFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                synchronized (zone) {
                    zone.notify();
                }
            }
        });

        synchronized (zone) {
            try {
                zone.wait(this.blockingTimeout * 3L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.defaultContactZone = zone.contactZone;
        return zone.contactZone;
    }

    /**
     * 获取指定的联系人分区。
     *
     * @param zoneName
     * @param successHandler
     * @param failureHandler
     */
    public void getContactZone(String zoneName, ContactZoneHandler successHandler, FailureHandler failureHandler) {
        // 从数据库里读取
        final ContactZone zone = this.storage.readContactZone(zoneName);
        if (null != zone) {
            if (zone.isValid()) {
                // 填充数据
                this.fillContactZone(zone);

                // 回调
                if (successHandler.isInMainThread()) {
                    this.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            successHandler.handleContactZone(zone);
                        }
                    });
                }
                else {
                    this.execute(new Runnable() {
                        @Override
                        public void run() {
                            successHandler.handleContactZone(zone);
                        }
                    });
                }
                return;
            }
        }

        // 数据库没有数据或已经过期，从服务器获取
        ContactZone dummyZone = new ContactZone(0L, zoneName, zoneName, 0, ContactZoneState.Normal);
        // 从服务器获取
        this.refreshContactZone(dummyZone, false, new StableContactZoneHandler() {
            @Override
            public void handleContactZone(ContactZone contactZone) {
                // 填充数据
                fillContactZone(contactZone);

                if (successHandler.isInMainThread()) {
                    executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            successHandler.handleContactZone(contactZone);
                        }
                    });
                }
                else {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            successHandler.handleContactZone(contactZone);
                        }
                    });
                }
            }
        }, new StableFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                if (failureHandler.isInMainThread()) {
                    executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            failureHandler.handleFailure(module, error);
                        }
                    });
                }
                else {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            failureHandler.handleFailure(module, error);
                        }
                    });
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

    private void fillContactZone(final ContactZone zone) {
        AtomicInteger count = new AtomicInteger(zone.getParticipants().size());

        ContactHandler successHandler = new DefaultContactHandler() {
            @Override
            public void handleContact(Contact contact) {
                zone.matchContact(contact);

                if (0 == count.decrementAndGet()) {
                    synchronized (count) {
                        count.notify();
                    }
                }
            }
        };

        FailureHandler failureHandler = new StableFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                if (0 == count.decrementAndGet()) {
                    synchronized (count) {
                        count.notify();
                    }
                }
            }
        };

        for (ContactZoneParticipant participant : zone.getParticipants()) {
            if (null == participant.getContact()) {
                this.getContact(participant.getContactId(), successHandler, failureHandler);
            }
            else {
                count.decrementAndGet();
            }
        }

        if (count.get() > 0) {
            synchronized (count) {
                try {
                    count.wait(this.blockingTimeout * 2L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 从服务器上刷新联系人分区数据。
     *
     * @param zone
     */
    private void refreshContactZone(ContactZone zone, StableContactZoneHandler successHandler,
                                    StableFailureHandler failureHandler) {
        this.refreshContactZone(zone, true, successHandler, failureHandler);
    }

    /**
     * 从服务器上刷新联系人分区数据。
     *
     * @param zone
     * @param compact
     */
    private void refreshContactZone(ContactZone zone, boolean compact,
                                    StableContactZoneHandler successHandler,
                                    StableFailureHandler failureHandler) {
        JSONObject data = new JSONObject();
        try {
            data.put("name", zone.name);
            data.put("compact", compact);
        } catch (JSONException e) {
            // Nothing
        }
        Packet requestPacket = new Packet(ContactServiceAction.GetContactZone, data);
        this.pipeline.send(ContactService.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    LogUtils.w(ContactService.class.getSimpleName(),
                            "#refreshContactZone - error : " + packet.state.code);
                    if (null != failureHandler) {
                        failureHandler.handleFailure(ContactService.this,
                                new ModuleError(ContactService.NAME, packet.state.code));
                    }
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    LogUtils.w(ContactService.class.getSimpleName(),
                            "#refreshContactZone - error : " + stateCode);
                    if (null != failureHandler) {
                        failureHandler.handleFailure(ContactService.this,
                                new ModuleError(ContactService.NAME, stateCode));
                    }
                    return;
                }

                try {
                    ContactZone newZone = new ContactZone(packet.extractServiceData());
                    if (compact) {
                        // 判断是否需要更新
                        if (newZone.getTimestamp() != zone.getTimestamp()) {
                            // 时间戳不一致，更新数据
                            execute(new Runnable() {
                                @Override
                                public void run() {
                                    refreshContactZone(zone, false, successHandler, failureHandler);
                                }
                            });
                        }
                        else {
                            if (null != successHandler) {
                                successHandler.handleContactZone(zone);
                            }
                        }
                    }
                    else {
                        // 更新数据
                        storage.writeContactZone(newZone);

                        if (null != successHandler) {
                            successHandler.handleContactZone(newZone);
                        }

                        ObservableEvent event = new ObservableEvent(ContactServiceEvent.ContactZoneUpdated, newZone);
                        notifyObservers(event);
                    }
                } catch (JSONException e) {
                    LogUtils.w(ContactService.class.getSimpleName(), e);
                }
            }
        });
    }

    private void listContactZones(long timestamp, ContactZoneListHandler handler) {
        JSONObject requestParam = new JSONObject();
        try {
            requestParam.put("timestamp", timestamp);
        } catch (JSONException e) {
            LogUtils.w(ContactService.class.getSimpleName(), e);
        }

        Packet request = new Packet(ContactServiceAction.ListContactZones, requestParam);
        this.pipeline.send(ContactService.NAME, request, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    LogUtils.w(ContactService.class.getSimpleName(), "#listContactZones error : " + packet.state.code);
                    handler.handleList(new ArrayList<>());
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    LogUtils.w(ContactService.class.getSimpleName(), "#listContactZones error : " + stateCode);
                    handler.handleList(new ArrayList<>());
                    return;
                }

                List<ContactZone> resultList = new ArrayList<>();
                JSONObject responseData = packet.extractServiceData();
                try {
                    JSONArray array = responseData.getJSONArray("list");
                    for (int i = 0; i < array.length(); ++i) {
                        JSONObject data = array.getJSONObject(i);
                        // 实例化
                        ContactZone zone = new ContactZone(data);

                        // 写入数据库
                        boolean exists = storage.writeContactZone(zone);
                        if (!exists) {
                            // 对于不存在数据直接调用更新
                            ObservableEvent event = new ObservableEvent(ContactServiceEvent.ContactZoneUpdated, zone);
                            notifyObservers(event);
                        }

                        resultList.add(zone);
                    }
                } catch (JSONException e) {
                    LogUtils.w(ContactService.class.getSimpleName(), e);
                }

                handler.handleList(resultList);
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

    @Override
    public void notifyObservers(ObservableEvent event) {
        super.notifyObservers(event);

        if (null != this.contactZoneListenerList && !this.contactZoneListenerList.isEmpty()) {
            if (ContactServiceEvent.ContactZoneUpdated.equals(event.getName())) {
                // 联系人分区已更新
                this.execute(new Runnable() {
                    @Override
                    public void run() {
                        ContactZone contactZone = (ContactZone) event.getData();
                        fillContactZone(contactZone);

                        executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                for (ContactZoneListener listener : contactZoneListenerList) {
                                    listener.onContactZoneUpdated(contactZone, ContactService.this);
                                }
                            }
                        });
                    }
                });
            }
        }
    }

    private void fireSignInCompleted() {
        LogUtils.d(ContactService.class.getSimpleName(), "#fireSignInCompleted");

        if (this.signInReady.get()) {
            return;
        }

        this.signInReady.set(true);

        // 写入数据库
        this.storage.writeContact(this.self);

        // 通知 Sign-In 事件
        ObservableEvent event = new ObservableEvent(ContactServiceEvent.SignIn, self);
        notifyObservers(event);

        if (null != this.signInHandler) {
            this.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    signInHandler.handleSuccess(ContactService.this, self);
                    signInHandler = null;
                }
            });
        }
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
        MutableBoolean gotZoneList = new MutableBoolean(false);
        MutableBoolean gotGroups = new MutableBoolean(false);
        MutableBoolean gotBlockList = new MutableBoolean(false);

        CompletionHandler completion = (module) -> {
            if (gotAppendix.value && gotZoneList.value && gotGroups.value && gotBlockList.value) {
                fireSignInCompleted();
            }
        };

        long now = System.currentTimeMillis();

        // 获取附录
        this.getAppendix(this.self, new DefaultContactAppendixHandler() {
            @Override
            public void handleAppendix(Contact contact, ContactAppendix appendix) {
                LogUtils.d(ContactService.class.getSimpleName(), "#getAppendix");
                gotAppendix.value = true;
                completion.handleCompletion(null);
            }
        }, new DefaultFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                LogUtils.d(ContactService.class.getSimpleName(), "#getAppendix error : " + error.code);
                gotAppendix.value = true;
                completion.handleCompletion(null);
            }
        });

        // 更新联系人分区数据
        long timestamp = this.storage.queryLastContactZoneTimestamp();
        if (timestamp == 0) {
            timestamp = now - this.retrospectDuration;
        }
        this.listContactZones(timestamp, new ContactZoneListHandler() {
            @Override
            public void handleList(List<ContactZone> list) {
                LogUtils.d(ContactService.class.getSimpleName(), "#listContactZones");
                gotZoneList.value = true;
                completion.handleCompletion(null);
            }
        });

        // 更新群组列表
        this.listGroups(now - this.retrospectDuration, now, new GroupListHandler() {
            @Override
            public void handleList(List<Group> groupList) {
                LogUtils.d(ContactService.class.getSimpleName(), "#listGroups");
                gotGroups.value = true;
                completion.handleCompletion(null);
            }
        });

        // 更新阻止清单
        this.listBlockList(new ContactListHandler() {
            @Override
            public void handleList(List<Contact> contactList) {
                LogUtils.d(ContactService.class.getSimpleName(), "#listBlockList");
                gotBlockList.value = true;
                completion.handleCompletion(null);
            }
        });
    }

    protected void triggerSignOut() {
        ObservableEvent event = new ObservableEvent(ContactServiceEvent.SignOut, this.self);
        this.notifyObservers(event);

        this.self = null;
    }
}
