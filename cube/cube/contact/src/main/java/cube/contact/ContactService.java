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

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import cube.auth.AuthToken;
import cube.contact.handler.ContactHandler;
import cube.contact.handler.ContactListHandler;
import cube.contact.handler.ContactZoneHandler;
import cube.contact.handler.ContactZoneListHandler;
import cube.contact.handler.ContactZoneParticipantHandler;
import cube.contact.handler.DefaultContactZoneHandler;
import cube.contact.handler.GroupHandler;
import cube.contact.handler.SignHandler;
import cube.contact.handler.StableContactAppendixHandler;
import cube.contact.handler.StableContactHandler;
import cube.contact.handler.StableContactZoneHandler;
import cube.contact.handler.StableGroupAppendixHandler;
import cube.contact.handler.StableGroupHandler;
import cube.contact.model.AbstractContact;
import cube.contact.model.Contact;
import cube.contact.model.ContactAppendix;
import cube.contact.model.ContactZone;
import cube.contact.model.ContactZoneBundle;
import cube.contact.model.ContactZoneParticipant;
import cube.contact.model.ContactZoneParticipantState;
import cube.contact.model.ContactZoneParticipantType;
import cube.contact.model.ContactZoneState;
import cube.contact.model.Group;
import cube.contact.model.GroupAppendix;
import cube.contact.model.GroupBundle;
import cube.contact.model.GroupState;
import cube.contact.model.MutableContact;
import cube.contact.model.MutableContactZone;
import cube.contact.model.MutableGroup;
import cube.contact.model.Self;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.Packet;
import cube.core.PipelineState;
import cube.core.handler.CompletionHandler;
import cube.core.handler.DefaultCompletionHandler;
import cube.core.handler.DefaultFailureHandler;
import cube.core.handler.FailureHandler;
import cube.core.handler.PipelineHandler;
import cube.core.handler.StableCompletionHandler;
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

    private final static String TAG = ContactService.class.getSimpleName();

    /** 阻塞调用方法的超时时间。 */
    private final long blockingTimeout = 10L * 1000L;

    /** 默认提供的联系人分区名称，签入时会自动加载该分区。 */
    private final String defaultContactZoneName = "contacts";

    /** 默认提供的群组分区名称，签入时会自动加载该分区。 */
    private final String defaultGroupZoneName = "groups";

    private long retrospectDuration = 30L * 24L * 60L * 60000L;

    private ContactStorage storage;

    private ContactPipelineListener pipelineListener;

    protected AtomicBoolean signInReady;

    private SignHandler signInHandler;

    protected Self self;

    private ContactZone defaultContactZone;

    private ContactZone defaultGroupZone;

    private ContactDataProvider contactDataProvider;

    protected ConcurrentHashMap<Long, AbstractContact> cache;

    protected ConcurrentHashMap<String, ContactZone> zoneCache;

    private List<ContactZoneListener> contactZoneListenerList;

    /**
     * 正在工作的群组清单句柄。
     */
    protected WorkingGroupListHandler workingGroupListHandler;

    public ContactService() {
        super(ContactService.NAME);
        this.signInReady = new AtomicBoolean(false);
        this.cache = new ConcurrentHashMap<>();
        this.zoneCache = new ConcurrentHashMap<>();
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
        this.kernel.getInspector().depositMap(this.zoneCache);

        return true;
    }

    @Override
    public void stop() {
        super.stop();

        this.kernel.getInspector().withdrawMap(this.cache);
        this.kernel.getInspector().withdrawMap(this.zoneCache);
        this.cache.clear();
        this.zoneCache.clear();

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
    protected void config(@Nullable JSONObject configData) {
        // Nothing
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
                    this.self.resetLast(System.currentTimeMillis());
                    this.storage.updateContactContext(this.self);
                }
                else {
                    this.self.setContext(contact.getContext());
                }

                this.execute(() -> {
                    ObservableEvent event = new ObservableEvent(ContactServiceEvent.SelfReady, ContactService.this.self);
                    notifyObservers(event);

                    if (null != handler) {
                        if (handler.isInMainThread()) {
                            executeOnMainThread(() -> {
                                handler.handleSuccess(ContactService.this, ContactService.this.self);
                            });
                        }
                        else {
                            execute(() -> {
                                handler.handleSuccess(ContactService.this, ContactService.this.self);
                            });
                        }
                    }
                });
            }
            else {
                if (null != handler) {
                    if (handler.isInMainThread()) {
                        this.executeOnMainThread(() -> {
                            handler.handleFailure(ContactService.this,
                                    new ModuleError(ContactService.NAME, ContactServiceState.NoNetwork.code));
                        });
                    }
                    else {
                        this.execute(() -> {
                            handler.handleFailure(ContactService.this,
                                    new ModuleError(ContactService.NAME, ContactServiceState.NoNetwork.code));
                        });
                    }
                }
            }

            return true;
        }

        // 访问服务器进行签入

        // 激活令牌
        AuthToken authToken = this.kernel.activeToken(self.id);
        if (null != authToken) {
            // 通知系统 Self 实例就绪
            this.execute(() -> {
                ObservableEvent event = new ObservableEvent(ContactServiceEvent.SelfReady, ContactService.this.self);
                notifyObservers(event);
            });

            if (null != handler) {
                // 设置回调
                this.signInHandler = handler;
            }

            // 更新上下文
            this.self.resetLast(System.currentTimeMillis());
            this.storage.updateContactContext(this.self);

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
                if (handler.isInMainThread()) {
                    this.executeOnMainThread(() -> {
                        handler.handleFailure(ContactService.this,
                                new ModuleError(ContactService.NAME, ContactServiceState.InconsistentToken.code));
                    });
                }
                else {
                    this.execute(() -> {
                        handler.handleFailure(ContactService.this,
                                new ModuleError(ContactService.NAME, ContactServiceState.InconsistentToken.code));
                    });
                }
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

            if (handler.isInMainThread()) {
                this.executeOnMainThread(() -> {
                    ObservableEvent event = new ObservableEvent(ContactServiceEvent.SignOut, self);
                    notifyObservers(event);

                    handler.handleSuccess(ContactService.this, self);
                    self = null;
                    // 清空缓存
                    cache.clear();
                });
            }
            else {
                this.execute(() -> {
                    ObservableEvent event = new ObservableEvent(ContactServiceEvent.SignOut, self);
                    notifyObservers(event);

                    handler.handleSuccess(ContactService.this, self);
                    self = null;
                    // 清空缓存
                    cache.clear();
                });
            }

            return true;
        }

        Packet signOutPacket = new Packet(ContactServiceAction.SignOut, this.self.toJSON());
        this.pipeline.send(ContactService.NAME, signOutPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    if (handler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            ModuleError error = new ModuleError(ContactService.NAME, packet.state.code);
                            handler.handleFailure(ContactService.this, error);
                        });
                    }
                    else {
                        execute(() -> {
                            ModuleError error = new ModuleError(ContactService.NAME, packet.state.code);
                            handler.handleFailure(ContactService.this, error);
                        });
                    }

                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    if (handler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            ModuleError error = new ModuleError(ContactService.NAME, stateCode);
                            handler.handleFailure(ContactService.this, error);
                        });
                    }
                    else {
                        execute(() -> {
                            ModuleError error = new ModuleError(ContactService.NAME, stateCode);
                            handler.handleFailure(ContactService.this, error);
                        });
                    }

                    return;
                }

                signInReady.set(false);

                final Self current = self;
                if (handler.isInMainThread()) {
                    executeOnMainThread(() -> {
                        handler.handleSuccess(ContactService.this, current);
                    });
                }
                else {
                    execute(() -> {
                        handler.handleSuccess(ContactService.this, current);
                    });
                }

                execute(() -> {
                    ObservableEvent event = new ObservableEvent(ContactServiceEvent.SignOut, self);
                    notifyObservers(event);

                    self = null;

                    // 清空缓存
                    cache.clear();

                    // 关闭存储器
                    storage.close();
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
     * 修改当前登录用户自己的信息。
     *
     * @param name 指定待修改的名称。
     * @param context 指定待修改的上下文数据。
     * @return 返回当前签入的联系人。
     */
    public Self modifySelf(String name, JSONObject context) {
        if (null == name && null == context) {
            return this.self;
        }

        if (null != name && name.length() > 2) {
            this.self.setName(name);
            this.self.resetLast(System.currentTimeMillis());
            this.storage.updateContactName(this.self);
        }

        if (null != context) {
            this.self.setContext(context);
            this.self.resetLast(System.currentTimeMillis());
            this.storage.updateContactContext(this.self);
        }

        if (!this.pipeline.isReady()) {
            return this.self;
        }

        JSONObject payload = new JSONObject();
        try {
            if (null != name) {
                payload.put("name", name);
            }

            if (null != context) {
                payload.put("context", context);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet request = new Packet(ContactServiceAction.ModifyContact, payload);
        this.pipeline.send(ContactService.NAME, request, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    synchronized (payload) {
                        payload.notify();
                    }
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    synchronized (payload) {
                        payload.notify();
                    }
                    return;
                }

                if (LogUtils.isDebugLevel()) {
                    LogUtils.d(TAG, "Modify self");
                }

                synchronized (payload) {
                    payload.notify();
                }
            }
        });

        synchronized (payload) {
            try {
                payload.wait(this.blockingTimeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return this.self;
    }

    /**
     * 获取本地存储的联系人。
     *
     * @param contactId 指定联系人 ID 。
     * @return 返回找到的联系人实例。
     */
    public Contact getLocalContact(Long contactId) {
        if (null == this.self) {
            return null;
        }

        if (contactId.longValue() == this.self.id.longValue()) {
            return this.self;
        }

        // 从缓存里读取
        AbstractContact abstractContact = this.cache.get(contactId);
        if (null != abstractContact) {
            if (!(abstractContact instanceof Contact)) {
                return null;
            }

            // 更新缓存寿命
            abstractContact.entityLifeExpiry += LIFESPAN;
            return (Contact) abstractContact;
        }

        // 读取数据库
        Contact contact = this.storage.readContact(contactId);
        if (null != contact) {
            contact.entityLifeExpiry += LIFESPAN;
            this.cache.put(contact.id, contact);
        }

        return contact;
    }

    /**
     * 将指定联系人保存到本地。
     *
     * @param contact 指定联系人。
     */
    public void saveLocalContact(Contact contact) {
        // 更新数据库
        this.storage.writeContact(contact);

        AbstractContact current = this.cache.get(contact.id);
        if (null != current) {
            current.entityLifeExpiry += LIFESPAN;

            current.setName(contact.getName());
            if (null != contact.getContext()) {
                current.setContext(contact.getContext());
            }
        }
    }

    /**
     * 获取指定 ID 的联系人。
     *
     * <b>不建议在主线程里调用该方法。</b>
     *
     * @param contactId 指定联系人 ID 。
     * @return 返回联系人实例。如果没有获取到数据返回 {@code null} 值。
     */
    public synchronized Contact getContact(Long contactId) {
        if (null == this.self) {
            return null;
        }

        if (contactId.longValue() == this.self.id.longValue()) {
            return this.self;
        }

        // 从缓存里读取
        AbstractContact abstractContact = this.cache.get(contactId);
        if (null != abstractContact) {
            if (!(abstractContact instanceof Contact)) {
                return null;
            }

            // 更新缓存寿命
            abstractContact.entityLifeExpiry += LIFESPAN;
            return (Contact) abstractContact;
        }

        final MutableContact mutableContact = new MutableContact();

        this.getContact(contactId, new StableContactHandler() {
            @Override
            public void handleContact(Contact contact) {
                // 赋值
                mutableContact.contact = contact;

                synchronized (mutableContact) {
                    mutableContact.notify();
                }
            }
        }, new StableFailureHandler() {
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
                Runnable callback = () -> {
                    ModuleError error = new ModuleError(ContactService.NAME, ContactServiceState.IllegalOperation.code);
                    failureHandler.handleFailure(ContactService.this, error);
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
                this.executeOnMainThread(() -> {
                    successHandler.handleContact(self);
                });
            }
            else {
                this.execute(() -> {
                    successHandler.handleContact(self);
                });
            }

            return;
        }

        // 从缓存里读取
        AbstractContact abstractContact = this.cache.get(contactId);
        if (null != abstractContact && abstractContact instanceof Contact) {
            // 更新缓存寿命
            abstractContact.entityLifeExpiry += LIFESPAN;

            if (successHandler.isInMainThread()) {
                this.executeOnMainThread(() -> {
                    successHandler.handleContact((Contact) abstractContact);
                });
            }
            else {
                this.execute(() -> {
                    successHandler.handleContact((Contact) abstractContact);
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
                        contact.resetLast(System.currentTimeMillis());
                        this.storage.updateContactContext(contact);
                    }
                    this.storage.updateContactName(contact);
                }

                // 写入缓存
                this.cache.put(contactId, contact);

                if (successHandler.isInMainThread()) {
                    this.executeOnMainThread(() -> {
                        successHandler.handleContact(contact);
                    });
                }
                else {
                    this.execute(() -> {
                        successHandler.handleContact(contact);
                    });
                }

                return;
            }
        }

        if (!this.pipeline.isReady()) {
            // 数据通道未就绪
            if (null != failureHandler) {
                if (failureHandler.isInMainThread()) {
                    this.executeOnMainThread(() -> {
                        ModuleError error = new ModuleError(NAME, ContactServiceState.NoNetwork.code);
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                }
                else {
                    this.execute(() -> {
                        ModuleError error = new ModuleError(NAME, ContactServiceState.NoNetwork.code);
                        failureHandler.handleFailure(ContactService.this, error);
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
            Log.w(TAG, "#getContact", e);
        }

        Packet requestPacket = new Packet(ContactServiceAction.GetContact, packetData);
        this.pipeline.send(ContactService.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    if (null != failureHandler) {
                        if (failureHandler.isInMainThread()) {
                            executeOnMainThread(() -> {
                                ModuleError error = new ModuleError(ContactService.NAME, ContactServiceState.ServerError.code);
                                failureHandler.handleFailure(ContactService.this, error);
                            });
                        }
                        else {
                            execute(() -> {
                                ModuleError error = new ModuleError(ContactService.NAME, ContactServiceState.ServerError.code);
                                failureHandler.handleFailure(ContactService.this, error);
                            });
                        }
                    }

                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    if (null != failureHandler) {
                        if (failureHandler.isInMainThread()) {
                            executeOnMainThread(() -> {
                                ModuleError error = new ModuleError(ContactService.NAME, stateCode);
                                failureHandler.handleFailure(ContactService.this, error);
                            });
                        }
                        else {
                            execute(() -> {
                                ModuleError error = new ModuleError(ContactService.NAME, stateCode);
                                failureHandler.handleFailure(ContactService.this, error);
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
                    getAppendix(contact, new StableContactAppendixHandler() {
                        @Override
                        public void handleAppendix(Contact contact, ContactAppendix appendix) {
                            // 写入缓存
                            cache.put(contactId, contact);

                            if (successHandler.isInMainThread()) {
                                executeOnMainThread(() -> {
                                    successHandler.handleContact(contact);
                                });
                            }
                            else {
                                successHandler.handleContact(contact);
                            }
                        }
                    }, new StableFailureHandler() {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            if (null != failureHandler) {
                                if (failureHandler.isInMainThread()) {
                                    executeOnMainThread(() -> {
                                        failureHandler.handleFailure(module, error);
                                    });
                                }
                                else {
                                    failureHandler.handleFailure(module, error);
                                }
                            }
                        }
                    });
                } catch (JSONException e) {
                    LogUtils.w(TAG, e);
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
        if (null == this.self) {
            return null;
        }

        if (null != this.defaultContactZone) {
            return this.defaultContactZone;
        }

        final MutableContactZone zone = new MutableContactZone();

        this.getContactZone(this.defaultContactZoneName, new DefaultContactZoneHandler(false) {
            @Override
            public void handleContactZone(ContactZone contactZone) {
                zone.contactZone = contactZone;

                synchronized (zone) {
                    zone.notify();
                }
            }
        }, new DefaultFailureHandler(false) {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                synchronized (zone) {
                    zone.notify();
                }
            }
        });

        synchronized (zone) {
            try {
                zone.wait(this.blockingTimeout + this.blockingTimeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.defaultContactZone = zone.contactZone;

        if (null != this.defaultContactZone) {
            // 检查参与人实例
            for (ContactZoneParticipant participant : this.defaultContactZone.getParticipants()) {
                if (null == participant.getContact()) {
                    this.defaultContactZone = null;
                    break;
                }
            }
        }

        return this.defaultContactZone;
    }

    /**
     * 获取默认的存储了群组的联系人分区。
     *
     * @return 返回默认的存储了群组的联系人分区。
     */
    public ContactZone getDefaultGroupZone() {
        if (null == this.self) {
            return null;
        }

        if (null != this.defaultGroupZone) {
            return this.defaultGroupZone;
        }

        final MutableContactZone zone = new MutableContactZone();

        this.getContactZone(this.defaultGroupZoneName, new DefaultContactZoneHandler(false) {
            @Override
            public void handleContactZone(ContactZone contactZone) {
                zone.contactZone = contactZone;

                synchronized (zone) {
                    zone.notify();
                }
            }
        }, new DefaultFailureHandler(false) {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                if (error.code == ContactServiceState.NotFindContactZone.code) {
                    // 没有找到该分区
                    // 创建分区
                    createContactZone(defaultGroupZoneName, false, new StableContactZoneHandler() {
                        @Override
                        public void handleContactZone(ContactZone contactZone) {
                            zone.contactZone = contactZone;

                            synchronized (zone) {
                                zone.notify();
                            }
                        }
                    }, new StableFailureHandler() {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            synchronized (zone) {
                                zone.notify();
                            }
                        }
                    });
                }
                else {
                    synchronized (zone) {
                        zone.notify();
                    }
                }
            }
        });

        synchronized (zone) {
            try {
                zone.wait(this.blockingTimeout + this.blockingTimeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.defaultGroupZone = zone.contactZone;

        return this.defaultGroupZone;
    }

    /**
     * 获取指定名称的联系人分区。
     *
     * @param zoneName
     * @return
     */
    public ContactZone getContactZone(String zoneName) {
        if (this.defaultContactZoneName.equals(zoneName)) {
            return this.defaultContactZone;
        }
        else if (this.defaultGroupZoneName.equals(zoneName)) {
            return this.defaultGroupZone;
        }

        ContactZone zone = this.zoneCache.get(zoneName);
        if (null != zone) {
            zone.entityLifeExpiry += LIFESPAN;
            return zone;
        }

        zone = this.storage.readContactZone(zoneName);
        if (null != zone) {
            // 填充数据
            this.fillContactZone(zone);

            this.zoneCache.put(zone.name, zone);
        }

        return zone;
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

                this.zoneCache.put(zone.name, zone);

                // 回调
                if (successHandler.isInMainThread()) {
                    this.executeOnMainThread(() -> {
                        successHandler.handleContactZone(zone);
                    });
                }
                else {
                    this.execute(() -> {
                        successHandler.handleContactZone(zone);
                    });
                }
                return;
            }
        }

        // 数据库没有数据或已经过期，从服务器获取
        ContactZone dummyZone = new ContactZone(this, 0L, zoneName, zoneName,
                false, 0, ContactZoneState.Normal);
        // 从服务器获取
        this.refreshContactZone(dummyZone, false, new StableContactZoneHandler() {
            @Override
            public void handleContactZone(ContactZone contactZone) {
                // 填充数据
                fillContactZone(contactZone);

                if (successHandler.isInMainThread()) {
                    executeOnMainThread(() -> {
                        successHandler.handleContactZone(contactZone);
                    });
                }
                else {
                    execute(() -> {
                        successHandler.handleContactZone(contactZone);
                    });
                }
            }
        }, new StableFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                if (failureHandler.isInMainThread()) {
                    executeOnMainThread(() -> {
                        failureHandler.handleFailure(module, error);
                    });
                }
                else {
                    execute(() -> {
                        failureHandler.handleFailure(module, error);
                    });
                }
            }
        });
    }

    /**
     * 创建指定名称的联系人分区。
     *
     * @param zoneName 指定分区名称。
     * @param peerMode 指定对等模式。
     * @param successHandler 指定操作成功回调句柄。
     * @param failureHandler 指定操作失败回调句柄。
     */
    public void createContactZone(String zoneName, boolean peerMode,
                                  ContactZoneHandler successHandler, FailureHandler failureHandler) {
        ContactZone zone = this.storage.readContactZone(zoneName);
        if (null != zone) {
            ModuleError error = new ModuleError(NAME, ContactServiceState.AlreadyExists.code);
            if (failureHandler.isInMainThread()) {
                executeOnMainThread(() -> {
                    failureHandler.handleFailure(ContactService.this, error);
                });
            }
            else {
                execute(() -> {
                    failureHandler.handleFailure(ContactService.this, error);
                });
            }
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("name", zoneName);
            payload.put("displayName", zoneName);
            payload.put("peerMode", peerMode);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(ContactServiceAction.CreateContactZone, payload);
        this.pipeline.send(ContactService.NAME, requestPacket, (packet) -> {
            if (packet.state.code != PipelineState.Ok.code) {
                ModuleError error = new ModuleError(NAME, packet.state.code);
                if (failureHandler.isInMainThread()) {
                    executeOnMainThread(() -> {
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                }
                else {
                    execute(() -> {
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                }
                return;
            }

            int stateCode = packet.extractServiceStateCode();
            if (stateCode != ContactServiceState.Ok.code) {
                ModuleError error = new ModuleError(NAME, stateCode);
                if (failureHandler.isInMainThread()) {
                    executeOnMainThread(() -> {
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                }
                else {
                    execute(() -> {
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                }
                return;
            }

            try {
                ContactZone responseZone = new ContactZone(ContactService.this, packet.extractServiceData());
                // 更新到数据库
                storage.writeContactZone(responseZone);

                if (successHandler.isInMainThread()) {
                    executeOnMainThread(() -> {
                        successHandler.handleContactZone(responseZone);
                    });
                }
                else {
                    execute(() -> {
                        successHandler.handleContactZone(responseZone);
                    });
                }
            } catch (JSONException e) {
                LogUtils.w(TAG, "#createContactZone", e);
            }
        });
    }

    /**
     * 向指定分区添加参与人。
     *
     * @param zone 指定联系人分区。
     * @param participant 指定待添加的参与人。
     * @param successHandler 指定操作成功回调句柄。
     * @param failureHandler 指定操作失败回调句柄。
     */
    public void addParticipantToZone(ContactZone zone, ContactZoneParticipant participant,
                                     ContactZoneHandler successHandler, FailureHandler failureHandler) {
        if (!this.pipeline.isReady()) {
            ModuleError error = new ModuleError(NAME, ContactServiceState.NoNetwork.code);
            error.data = zone;
            if (failureHandler.isInMainThread()) {
                executeOnMainThread(() -> {
                    failureHandler.handleFailure(ContactService.this, error);
                });
            }
            else {
                execute(() -> {
                    failureHandler.handleFailure(ContactService.this, error);
                });
            }
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("name", zone.name);
            payload.put("participant", participant.toJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(ContactServiceAction.AddParticipantToZone, payload);
        this.pipeline.send(ContactService.NAME, requestPacket, (packet) -> {
            if (packet.state.code != PipelineState.Ok.code) {
                ModuleError error = new ModuleError(NAME, packet.state.code);
                error.data = zone;
                if (failureHandler.isInMainThread()) {
                    executeOnMainThread(() -> {
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                }
                else {
                    execute(() -> {
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                }
                return;
            }

            int stateCode = packet.extractServiceStateCode();
            if (stateCode != ContactServiceState.Ok.code) {
                ModuleError error = new ModuleError(NAME, stateCode);
                error.data = zone;
                if (failureHandler.isInMainThread()) {
                    executeOnMainThread(() -> {
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                }
                else {
                    execute(() -> {
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                }
                return;
            }

            JSONObject response = packet.extractServiceData();
            long timestamp = 0;
            try {
                timestamp = response.getLong("timestamp");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // 移除
            zone.addParticipant(participant);
            zone.resetLast(timestamp);
            zone.setTimestamp(timestamp);

            // 更新数据库
            storage.addParticipant(zone, participant);

            if (successHandler.isInMainThread()) {
                executeOnMainThread(() -> {
                    successHandler.handleContactZone(zone);
                });
            }
            else {
                execute(() -> {
                    successHandler.handleContactZone(zone);
                });
            }

            execute(() -> {
                ContactZoneBundle bundle = new ContactZoneBundle(zone, participant);
                ObservableEvent event = new ObservableEvent(ContactServiceEvent.ZoneParticipantAdded, bundle);
                notifyObservers(event);

                event = new ObservableEvent(ContactServiceEvent.ContactZoneUpdated, zone);
                notifyObservers(event);
            });
        });
    }

    /**
     * 从指定分区里移除参与人。
     *
     * @param zone 指定联系人分区。
     * @param participant 指定待移除的参与人。
     * @param successHandler 指定操作成功回调句柄。
     * @param failureHandler 指定操作失败回调句柄。
     */
    public void removeParticipantFromZone(ContactZone zone, ContactZoneParticipant participant,
                                          ContactZoneHandler successHandler, FailureHandler failureHandler) {
        if (!this.pipeline.isReady()) {
            ModuleError error = new ModuleError(NAME, ContactServiceState.NoNetwork.code);
            error.data = zone;
            if (failureHandler.isInMainThread()) {
                executeOnMainThread(() -> {
                    failureHandler.handleFailure(ContactService.this, error);
                });
            }
            else {
                execute(() -> {
                    failureHandler.handleFailure(ContactService.this, error);
                });
            }
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("name", zone.name);
            payload.put("participant", participant.toCompactJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(ContactServiceAction.RemoveParticipantFromZone, payload);
        this.pipeline.send(ContactService.NAME, requestPacket, (packet) -> {
            if (packet.state.code != PipelineState.Ok.code) {
                ModuleError error = new ModuleError(NAME, packet.state.code);
                error.data = zone;
                if (failureHandler.isInMainThread()) {
                    executeOnMainThread(() -> {
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                }
                else {
                    execute(() -> {
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                }
                return;
            }

            int stateCode = packet.extractServiceStateCode();
            if (stateCode != ContactServiceState.Ok.code) {
                ModuleError error = new ModuleError(NAME, stateCode);
                error.data = zone;
                if (failureHandler.isInMainThread()) {
                    executeOnMainThread(() -> {
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                }
                else {
                    execute(() -> {
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                }
                return;
            }

            JSONObject response = packet.extractServiceData();
            long timestamp = 0;
            try {
                timestamp = response.getLong("timestamp");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // 移除
            zone.removeParticipant(participant);
            zone.resetLast(timestamp);
            zone.setTimestamp(timestamp);

            // 更新数据库
            storage.removeParticipant(zone, participant);

            if (successHandler.isInMainThread()) {
                executeOnMainThread(() -> {
                    successHandler.handleContactZone(zone);
                });
            }
            else {
                execute(() -> {
                    successHandler.handleContactZone(zone);
                });
            }

            execute(() -> {
                ContactZoneBundle bundle = new ContactZoneBundle(zone, participant);
                ObservableEvent event = new ObservableEvent(ContactServiceEvent.ZoneParticipantRemoved, bundle);
                notifyObservers(event);

                event = new ObservableEvent(ContactServiceEvent.ContactZoneUpdated, zone);
                notifyObservers(event);
            });
        });
    }

    /**
     * 修改制定参与人的状态。
     *
     * @param zone
     * @param participant
     * @param state
     * @param successHandler
     * @param failureHandler
     */
    public void modifyParticipant(ContactZone zone, ContactZoneParticipant participant, ContactZoneParticipantState state,
                                  ContactZoneParticipantHandler successHandler,
                                  FailureHandler failureHandler) {
        // 判断是否能修改
        if (participant.isInviter()) {
            // "我"是邀请人不能修改状态
            this.execute(failureHandler);
            return;
        }

        if (!this.pipeline.isReady()) {
            ModuleError error = new ModuleError(NAME, ContactServiceState.NoNetwork.code);
            error.data = zone;
            this.execute(failureHandler, error);
            return;
        }

        // 原状态
        ContactZoneParticipantState currentState = participant.getState();

        // 如果状态相同，则返回成功
        if (state == currentState) {
            if (successHandler.isInMainThread()) {
                executeOnMainThread(() -> {
                    successHandler.handleContactZoneParticipant(participant, zone);
                });
            }
            else {
                execute(() -> {
                    successHandler.handleContactZoneParticipant(participant, zone);
                });
            }
            return;
        }

        // 设置新状态
        participant.setState(state);

        JSONObject payload = new JSONObject();
        try {
            payload.put("name", zone.name);
            payload.put("participant", participant.toCompactJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(ContactServiceAction.ModifyZoneParticipant, payload);
        this.pipeline.send(ContactService.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    // 设置失败，还原状态
                    participant.setState(currentState);
                    ModuleError error = new ModuleError(NAME, packet.state.code);
                    execute(failureHandler, error);
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    // 设置失败，还原状态
                    participant.setState(currentState);
                    ModuleError error = new ModuleError(NAME, stateCode);
                    execute(failureHandler, error);
                    return;
                }

                try {
                    ContactZoneParticipant response = new ContactZoneParticipant(packet.extractServiceData());
                    long timestamp = response.getTimestamp();

                    participant.setState(response.getState());
                    participant.setTimestamp(timestamp);

                    zone.resetLast(timestamp);
                    zone.setTimestamp(timestamp);

                    storage.updateParticipant(zone, participant);

                    // 更新缓存寿命
                    zone.entityLifeExpiry += LIFESPAN;
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (successHandler.isInMainThread()) {
                    executeOnMainThread(() -> {
                        successHandler.handleContactZoneParticipant(participant, zone);
                    });
                }
                else {
                    execute(() -> {
                        successHandler.handleContactZoneParticipant(participant, zone);
                    });
                }
            }
        });
    }

    /**
     * 创建群组。
     *
     * @param members 指定成员列表。
     * @param successHandler 指定创建成功回调句柄。
     * @param failureHandler 指定创建故障回调句柄。
     */
    public void createGroup(List<Contact> members, GroupHandler successHandler, FailureHandler failureHandler) {
        String name = this.self.getName() + "创建的群组";
        this.createGroup(name, members, successHandler, failureHandler);
    }

    /**
     * 创建群组。
     *
     * @param name 指定群组名称。
     * @param members 指定成员列表。
     * @param successHandler 指定创建成功回调句柄。
     * @param failureHandler 指定创建故障回调句柄。
     */
    public void createGroup(String name, List<Contact> members, GroupHandler successHandler, FailureHandler failureHandler) {
        if (!this.pipeline.isReady()) {
            ModuleError error = new ModuleError(NAME, ContactServiceState.NoNetwork.code);
            if (failureHandler.isInMainThread()) {
                executeOnMainThread(() -> {
                    failureHandler.handleFailure(ContactService.this, error);
                });
            }
            else {
                execute(() -> {
                    failureHandler.handleFailure(ContactService.this, error);
                });
            }
            return;
        }

        Group rawGroup = new Group(this.self.id, name);

        JSONObject payload = new JSONObject();
        try {
            payload.put("group", rawGroup.toCompactJSON());

            JSONArray array = new JSONArray();
            for (Contact member : members) {
                array.put(member.id.longValue());
            }
            payload.put("members", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(ContactServiceAction.CreateGroup, payload);
        this.pipeline.send(ContactService.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(NAME, packet.state.code);
                    if (failureHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    else {
                        execute(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    ModuleError error = new ModuleError(NAME, stateCode);
                    if (failureHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    else {
                        execute(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    return;
                }

                try {
                    Group group = new Group(packet.extractServiceData());

                    // 写入数据库
                    storage.writeGroup(group);

                    // 写入缓存
                    cache.put(group.id, group);

                    getAppendix(group, new StableGroupAppendixHandler() {
                        @Override
                        public void handleAppendix(Group group, GroupAppendix appendix) {
                            if (successHandler.isInMainThread()) {
                                executeOnMainThread(() -> {
                                    successHandler.handleGroup(group);
                                });
                            }
                            else {
                                successHandler.handleGroup(group);
                            }

                            ObservableEvent event = new ObservableEvent(ContactServiceEvent.GroupCreated, group);
                            notifyObservers(event);
                        }
                    }, new StableFailureHandler() {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            if (failureHandler.isInMainThread()) {
                                executeOnMainThread(() -> {
                                    failureHandler.handleFailure(module, error);
                                });
                            }
                            else {
                                failureHandler.handleFailure(module, error);
                            }
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 退出群组。如果本人是群主，则群组被解散。
     *
     * @param group 指定群组。
     * @param successHandler 指定操作成功回调句柄。
     * @param failureHandler 指定操作失败回调句柄。
     */
    public void quitGroup(Group group, GroupHandler successHandler, FailureHandler failureHandler) {
        if (group.getOwnerId().equals(this.self.id)) {
            // 群主是本人，解散群
            this.dismissGroup(group, successHandler, failureHandler);
            return;
        }

        if (!this.pipeline.isReady()) {
            ModuleError error = new ModuleError(NAME, ContactServiceState.NoNetwork.code);
            error.data = group;
            if (failureHandler.isInMainThread()) {
                executeOnMainThread(() -> {
                    failureHandler.handleFailure(ContactService.this, error);
                });
            }
            else {
                execute(() -> {
                    failureHandler.handleFailure(ContactService.this, error);
                });
            }
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("groupId", group.id.longValue());
            JSONArray list = new JSONArray();
            list.put(this.self.id.longValue());
            payload.put("memberIdList", list);
            payload.put("operator", this.self.toCompactJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(ContactServiceAction.RemoveGroupMember, payload);
        this.pipeline.send(ContactService.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(NAME, packet.state.code);
                    error.data = group;
                    if (failureHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    else {
                        execute(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    ModuleError error = new ModuleError(NAME, stateCode);
                    error.data = group;
                    if (failureHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    else {
                        execute(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    return;
                }

                GroupBundle bundle = new GroupBundle(packet.extractServiceData());
                storage.removeGroupMember(bundle);

                // 更新状态
                long now = System.currentTimeMillis();
                group.setState(GroupState.Disabled);
                group.setLastActive(now);
                group.resetLast(now);
                group.removeMember(self.id);

                storage.updateGroupProperty(group);

                // 从缓存里删除
                cache.remove(group.id);

                if (successHandler.isInMainThread()) {
                    executeOnMainThread(() -> {
                        successHandler.handleGroup(group);
                    });
                }
                else {
                    execute(() -> {
                        successHandler.handleGroup(group);
                    });
                }

                ObservableEvent event = new ObservableEvent(ContactServiceEvent.GroupQuit, group);
                notifyObservers(event);
            }
        });
    }

    /**
     * 解散群组。只有群主可以解散群。
     *
     * @param group 指定群组。
     * @param successHandler 指定操作成功回调句柄。
     * @param failureHandler 指定操作失败回调句柄。
     */
    public void dismissGroup(Group group, GroupHandler successHandler, FailureHandler failureHandler) {
        if (!this.pipeline.isReady()) {
            ModuleError error = new ModuleError(NAME, ContactServiceState.NoNetwork.code);
            error.data = group;
            if (failureHandler.isInMainThread()) {
                executeOnMainThread(() -> {
                    failureHandler.handleFailure(ContactService.this, error);
                });
            }
            else {
                execute(() -> {
                    failureHandler.handleFailure(ContactService.this, error);
                });
            }
            return;
        }

        // 解散群组
        Packet requestPacket = new Packet(ContactServiceAction.DismissGroup, group.toCompactJSON());
        this.pipeline.send(ContactService.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(NAME, packet.state.code);
                    error.data = group;
                    if (failureHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    else {
                        execute(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    ModuleError error = new ModuleError(NAME, stateCode);
                    error.data = group;
                    if (failureHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    else {
                        execute(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    return;
                }

                try {
                    Group responseGroup = new Group(packet.extractServiceData());
                    group.setState(responseGroup.getState());
                    group.setLastActive(responseGroup.getLastActive());
                    group.resetLast(System.currentTimeMillis());

                    // 更新状态
                    storage.updateGroupProperty(group);

                    if (successHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            successHandler.handleGroup(group);
                        });
                    }
                    else {
                        execute(() -> {
                            successHandler.handleGroup(group);
                        });
                    }

                    execute(() -> {
                        ObservableEvent event = new ObservableEvent(ContactServiceEvent.GroupDismissed, group);
                        notifyObservers(event);
                    });
                } catch (JSONException e) {
                    LogUtils.w(TAG, "dismiss group failed", e);
                }
            }
        });
    }

    /**
     * 修改群组名称。
     *
     * @param group 指定群组。
     * @param groupName 指定新的群组名。
     * @param successHandler 指定操作成功回调句柄。
     * @param failureHandler 指定操作故障回调句柄。
     */
    public void modifyGroupName(Group group, String groupName, GroupHandler successHandler, FailureHandler failureHandler) {
        String name = group.getName();
        if (name.equals(groupName)) {
            if (successHandler.isInMainThread()) {
                executeOnMainThread(() -> {
                    successHandler.handleGroup(group);
                });
            }
            else {
                execute(() -> {
                    successHandler.handleGroup(group);
                });
            }
            return;
        }

        group.setName(groupName);
        this.modifyGroup(group, successHandler, failureHandler);
    }

    /**
     * 修改群组信息。
     *
     * @param group
     * @param successHandler
     * @param failureHandler
     */
    private void modifyGroup(Group group, GroupHandler successHandler, FailureHandler failureHandler) {
        // 更新缓存时间
        group.entityLifeExpiry += LIFESPAN;

        Packet requestPacket = new Packet(ContactServiceAction.ModifyGroup, group.toCompactJSON());
        this.pipeline.send(ContactService.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(NAME, packet.state.code);
                    error.data = group;
                    if (failureHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    else {
                        execute(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    ModuleError error = new ModuleError(NAME, stateCode);
                    error.data = group;
                    if (failureHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    else {
                        execute(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    return;
                }

                try {
                    Group responseGroup = new Group(packet.extractServiceData());
                    group.resetLast(System.currentTimeMillis());
                    // 重置名称，因为服务器会对名字进行安全评估，新名称可能被服务器修改
                    group.update(responseGroup);

                    // 更新数据库
                    storage.updateGroupProperty(group);

                    if (successHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            successHandler.handleGroup(group);
                        });
                    }
                    else {
                        execute(() -> {
                            successHandler.handleGroup(group);
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 获取指定群组。
     *
     * @param groupId 指定群组 ID 。
     * @return 返回群组实例，如果没有该群组返回 {@code null} 值。
     */
    public Group getGroup(Long groupId) {
        if (null == this.self) {
            return null;
        }

        // 从缓存里读取
        AbstractContact abstractGroup = this.cache.get(groupId);
        if (null != abstractGroup) {
            if (!(abstractGroup instanceof Group)) {
                return null;
            }

            // 更新缓存寿命
            abstractGroup.entityLifeExpiry += LIFESPAN;

            Group group = (Group) abstractGroup;
            if (!group.isFilled()) {
                this.fillGroup(group);
            }

            return group;
        }

        final MutableGroup mutableGroup = new MutableGroup();

        this.getGroup(groupId, new StableGroupHandler() {
            @Override
            public void handleGroup(Group group) {
                mutableGroup.group = group;
                synchronized (mutableGroup) {
                    mutableGroup.notify();
                }
            }
        }, new StableFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                synchronized (mutableGroup) {
                    mutableGroup.notify();
                }
            }
        });

        synchronized (mutableGroup) {
            try {
                mutableGroup.wait(this.blockingTimeout * this.blockingTimeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return mutableGroup.group;
    }

    /**
     * 获取群组。
     *
     * @param groupId 指定群组 ID 。
     * @param successHandler 指定操作成功回调句柄。
     * @param failureHandler 指定操作故障回调句柄。
     */
    public void getGroup(Long groupId, GroupHandler successHandler, FailureHandler failureHandler) {
        if (null == this.self) {
            if (null != failureHandler) {
                Runnable callback = () -> {
                    ModuleError error = new ModuleError(ContactService.NAME, ContactServiceState.IllegalOperation.code);
                    failureHandler.handleFailure(ContactService.this, error);
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

        // 从缓存里读取
        AbstractContact abstractContact = this.cache.get(groupId);
        if (null != abstractContact && abstractContact instanceof Group) {
            // 更新缓存寿命
            abstractContact.entityLifeExpiry += LIFESPAN;

            Group group = (Group) abstractContact;
            if (!group.isFilled()) {
                fillGroup(group);
            }

            if (successHandler.isInMainThread()) {
                this.executeOnMainThread(() -> {
                    successHandler.handleGroup(group);
                });
            }
            else {
                this.execute(() -> {
                    successHandler.handleGroup(group);
                });
            }

            return;
        }

        // 从数据库读取
        Group group = this.storage.readGroup(groupId);
        if (null != group) {
            // 在没有连接服务器或者数据有效时返回
            if (!this.pipeline.isReady() || group.isValid()) {
                // 填充数据
                this.fillGroup(group);

                // 写入缓存
                this.cache.put(groupId, group);

                if (successHandler.isInMainThread()) {
                    this.executeOnMainThread(() -> {
                        successHandler.handleGroup(group);
                    });
                }
                else {
                    this.execute(() -> {
                        successHandler.handleGroup(group);
                    });
                }

                return;
            }
        }

        if (!this.pipeline.isReady()) {
            // 数据通道未就绪
            if (null != failureHandler) {
                if (failureHandler.isInMainThread()) {
                    this.executeOnMainThread(() -> {
                        ModuleError error = new ModuleError(NAME, ContactServiceState.NoNetwork.code);
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                }
                else {
                    this.execute(() -> {
                        ModuleError error = new ModuleError(NAME, ContactServiceState.NoNetwork.code);
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                }
            }

            return;
        }

        JSONObject packetData = new JSONObject();
        try {
            packetData.put("id", groupId.longValue());
            packetData.put("domain", this.getAuthToken().domain);
        } catch (JSONException e) {
            Log.w(TAG, "#getGroup", e);
        }

        Packet requestPacket = new Packet(ContactServiceAction.GetGroup, packetData);
        this.pipeline.send(ContactService.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    if (null != failureHandler) {
                        if (failureHandler.isInMainThread()) {
                            executeOnMainThread(() -> {
                                ModuleError error = new ModuleError(ContactService.NAME, ContactServiceState.ServerError.code);
                                failureHandler.handleFailure(ContactService.this, error);
                            });
                        }
                        else {
                            execute(() -> {
                                ModuleError error = new ModuleError(ContactService.NAME, ContactServiceState.ServerError.code);
                                failureHandler.handleFailure(ContactService.this, error);
                            });
                        }
                    }

                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    if (null != failureHandler) {
                        if (failureHandler.isInMainThread()) {
                            executeOnMainThread(() -> {
                                ModuleError error = new ModuleError(ContactService.NAME, stateCode);
                                failureHandler.handleFailure(ContactService.this, error);
                            });
                        }
                        else {
                            execute(() -> {
                                ModuleError error = new ModuleError(ContactService.NAME, stateCode);
                                failureHandler.handleFailure(ContactService.this, error);
                            });
                        }
                    }
                    return;
                }

                try {
                    JSONObject json = packet.extractServiceData();
                    json.put("domain", getAuthToken().domain);
                    Group group = new Group(json);

                    // 写入数据库
                    storage.writeGroup(group);

                    // 获取附录
                    getAppendix(group, new StableGroupAppendixHandler() {
                        @Override
                        public void handleAppendix(Group group, GroupAppendix appendix) {
                            // 填充数据
                            fillGroup(group);

                            // 写入缓存
                            cache.put(groupId, group);

                            if (successHandler.isInMainThread()) {
                                executeOnMainThread(() -> {
                                    successHandler.handleGroup(group);
                                });
                            }
                            else {
                                successHandler.handleGroup(group);
                            }
                        }
                    }, new StableFailureHandler() {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            if (null != failureHandler) {
                                if (failureHandler.isInMainThread()) {
                                    executeOnMainThread(() -> {
                                        failureHandler.handleFailure(module, error);
                                    });
                                }
                                else {
                                    failureHandler.handleFailure(module, error);
                                }
                            }
                        }
                    });
                } catch (JSONException e) {
                    LogUtils.w(TAG, e);
                }
            }
        });
    }

    /**
     * 更新指定参数的群组附录。
     *
     * @param appendix
     * @param params
     * @param successHandler
     * @param failureHandler
     */
    public void updateAppendix(GroupAppendix appendix, JSONObject params, GroupHandler successHandler, FailureHandler failureHandler) {
        if (!this.pipeline.isReady()) {
            ModuleError error = new ModuleError(NAME, ContactServiceState.NoNetwork.code);
            error.data = appendix.getGroup();
            if (failureHandler.isInMainThread()) {
                executeOnMainThread(() -> {
                    failureHandler.handleFailure(ContactService.this, error);
                });
            }
            else {
                execute(() -> {
                    failureHandler.handleFailure(ContactService.this, error);
                });
            }
            return;
        }

        try {
            params.put("groupId", appendix.getGroup().getId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Packet requestPacket = new Packet(ContactServiceAction.UpdateAppendix, params);
        this.pipeline.send(ContactService.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(NAME, packet.state.code);
                    error.data = appendix.getGroup();
                    if (failureHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    else {
                        execute(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    ModuleError error = new ModuleError(NAME, stateCode);
                    error.data = appendix.getGroup();
                    if (failureHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    else {
                        execute(() -> {
                            failureHandler.handleFailure(ContactService.this, error);
                        });
                    }
                    return;
                }

                try {
                    // 更新缓存时间
                    appendix.getGroup().entityLifeExpiry += LIFESPAN;

                    GroupAppendix response = new GroupAppendix(ContactService.this, appendix.getGroup(), packet.extractServiceData());
                    appendix.getGroup().setAppendix(response);

                    // 更新群组时间戳
                    appendix.getGroup().setLastActive(System.currentTimeMillis());

                    // 更新数据库
                    storage.updateGroupProperty(appendix.getGroup());
                    storage.writeAppendix(response);

                    if (successHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            successHandler.handleGroup(appendix.getGroup());
                        });
                    }
                    else {
                        execute(() -> {
                            successHandler.handleGroup(appendix.getGroup());
                        });
                    }

                    execute(() -> {
                        ObservableEvent event = new ObservableEvent(ContactServiceEvent.GroupAppendixUpdated, response);
                        notifyObservers(event);
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void execute(FailureHandler failureHandler) {
        ModuleError error = new ModuleError(NAME, ContactServiceState.IllegalOperation.code);
        this.execute(failureHandler, error);
    }

    public void execute(FailureHandler failureHandler, ModuleError error) {
        if (failureHandler.isInMainThread()) {
            executeOnMainThread(() -> {
                failureHandler.handleFailure(ContactService.this, error);
            });
        }
        else {
            execute(() -> {
                failureHandler.handleFailure(ContactService.this, error);
            });
        }
    }

    private void getAppendix(Contact contact, StableContactAppendixHandler successHandler, StableFailureHandler failureHandler) {
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
                    execute(() -> {
                        ModuleError error = new ModuleError(ContactService.NAME, packet.state.code);
                        error.data = contact;
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    execute(() -> {
                        ModuleError error = new ModuleError(ContactService.NAME, stateCode);
                        error.data = contact;
                        failureHandler.handleFailure(ContactService.this, error);
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

                    execute(() -> {
                        successHandler.handleAppendix(contact, appendix);
                    });
                }
                catch (JSONException e) {
                    execute(() -> {
                        ModuleError error = new ModuleError(ContactService.NAME, ContactServiceState.DataStructureError.code);
                        error.data = contact;
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                }
            }
        });
    }

    private void getAppendix(Group group, StableGroupAppendixHandler successHandler, StableFailureHandler failureHandler) {
        JSONObject data = new JSONObject();
        try {
            data.put("groupId", group.id.longValue());
        } catch (JSONException e) {
            // Nothing
        }
        Packet request = new Packet(ContactServiceAction.GetAppendix, data);
        this.pipeline.send(ContactService.NAME, request, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    execute(() -> {
                        ModuleError error = new ModuleError(ContactService.NAME, packet.state.code);
                        error.data = group;
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    execute(() -> {
                        ModuleError error = new ModuleError(ContactService.NAME, stateCode);
                        error.data = group;
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                    return;
                }

                try {
                    GroupAppendix appendix = new GroupAppendix(ContactService.this, group,
                            packet.extractServiceData());
                    // 更新存储
                    storage.writeAppendix(appendix);

                    // 赋值
                    group.setAppendix(appendix);

                    execute(() -> {
                        successHandler.handleAppendix(group, appendix);
                    });
                }
                catch (JSONException e) {
                    execute(() -> {
                        ModuleError error = new ModuleError(ContactService.NAME, ContactServiceState.DataStructureError.code);
                        error.data = group;
                        failureHandler.handleFailure(ContactService.this, error);
                    });
                }
            }
        });
    }

    private synchronized void fillContactZone(final ContactZone zone) {
        for (ContactZoneParticipant participant : zone.getParticipants()) {
            if (participant.getType() == ContactZoneParticipantType.Contact) {
                if (null == participant.getContact()) {
                    Contact contact = this.getContact(participant.getId());
                    if (null == contact) {
                        contact = new Contact(participant.id, "");
                    }
                    participant.setContact(contact);
                }
            }
            else if (participant.getType() == ContactZoneParticipantType.Group) {
                if (null == participant.getGroup()) {
                    Group group = this.getGroup(participant.getId());
                    participant.setGroup(group);
                }
            }

            // 邀请人
            Long inviterId = participant.getInviterId();
            if (inviterId.longValue() > 0 && null == participant.getInviter()) {
                participant.setInviter(this.getContact(inviterId), inviterId.longValue() == this.self.id.longValue());
            }
        }
    }

    private void fillGroup(final Group group) {
        for (Long memberId : group.getMemberIdList()) {
            if (null == group.getMember(memberId)) {
                Contact contact = this.getContact(memberId);
                if (null != contact) {
                    group.updateMember(contact);
                }
            }
        }

        group.setIsOwner(group.getOwnerId().equals(this.self.id));

        if (group.getAppendix().hasNotice()) {
            group.getAppendix().setNoticeOperator(this.getContact(group.getAppendix().getNoticeOperatorId()));
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
        if (!this.pipeline.isReady() || !this.isReady()) {
            this.execute(() -> {
                ModuleError error = new ModuleError(NAME, ContactServiceState.NoSignIn.code);
                error.data = zone;
                failureHandler.handleFailure(this, error);
            });
            return;
        }

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
                    LogUtils.w(TAG, "#refreshContactZone - error : " + packet.state.code);
                    if (null != failureHandler) {
                        failureHandler.handleFailure(ContactService.this,
                                new ModuleError(ContactService.NAME, packet.state.code));
                    }
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != ContactServiceState.Ok.code) {
                    LogUtils.w(TAG, "#refreshContactZone - error : " + stateCode);
                    if (null != failureHandler) {
                        failureHandler.handleFailure(ContactService.this,
                                new ModuleError(ContactService.NAME, stateCode));
                    }
                    return;
                }

                try {
                    ContactZone newZone = new ContactZone(ContactService.this, packet.extractServiceData());
                    if (compact) {
                        // 判断是否需要更新
                        if (newZone.getTimestamp() != zone.getTimestamp()) {
                            // 时间戳不一致，更新数据
                            execute(() -> {
                                refreshContactZone(newZone, false, successHandler, failureHandler);
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
                        newZone.resetLast(System.currentTimeMillis());
                        storage.writeContactZone(newZone);

                        // 填充数据
                        fillContactZone(newZone);

                        if (null != successHandler) {
                            successHandler.handleContactZone(newZone);
                        }

                        execute(() -> {
                            ObservableEvent event = new ObservableEvent(ContactServiceEvent.ContactZoneUpdated, newZone);
                            notifyObservers(event);
                        });
                    }
                } catch (JSONException e) {
                    LogUtils.w(TAG, e);
                }
            }
        });
    }

    /**
     * 从服务器更新分区信息。
     *
     * @param timestamp
     * @param handler
     */
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
                        ContactZone zone = new ContactZone(ContactService.this, data);

                        // 写入数据库
                        boolean exists = storage.writeContactZone(zone);
                        if (!exists) {
                            // 对于不存在数据直接调用更新
                            // 填充数据
                            fillContactZone(zone);

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

    private void listGroups(long beginning, long ending, CompletionHandler completionHandler) {
        if (null != this.workingGroupListHandler) {
            return;
        }

        this.workingGroupListHandler = new WorkingGroupListHandler(this, completionHandler);

        JSONObject payload = new JSONObject();
        try {
            payload.put("beginning", beginning);
            payload.put("ending", ending);
            payload.put("pageSize", 4);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Packet packet = new Packet(ContactServiceAction.ListGroups, payload);
        this.pipeline.send(ContactService.NAME, packet);
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
                this.execute(() -> {
                    ContactZone contactZone = (ContactZone) event.getData();
                    fillContactZone(contactZone);

                    executeOnMainThread(() -> {
                        for (ContactZoneListener listener : contactZoneListenerList) {
                            listener.onContactZoneUpdated(contactZone, ContactService.this);
                        }
                    });
                });
            }
        }
    }

    private void fireSignInCompleted() {
        LogUtils.d(TAG, "#fireSignInCompleted");

        if (this.signInReady.get()) {
            return;
        }

        this.signInReady.set(true);

        // 写入数据库
        this.storage.writeContact(this.self);

        // 通知 Sign-In 事件
        ObservableEvent event = new ObservableEvent(ContactServiceEvent.SignIn, this.self);
        notifyObservers(event);

        if (null != this.signInHandler) {
            if (this.signInHandler.isInMainThread()) {
                this.executeOnMainThread(() -> {
                    signInHandler.handleSuccess(ContactService.this, self);
                    signInHandler = null;
                });
            }
            else {
                this.execute(() -> {
                    signInHandler.handleSuccess(ContactService.this, self);
                    signInHandler = null;
                });
            }
        }

        (new Thread(() -> {
            // 获取默认联系人分区
            getDefaultContactZone();
            getDefaultGroupZone();
        })).start();
    }

    protected void triggerSignIn(Packet packet) {
        int stateCode = packet.extractServiceStateCode();
        JSONObject payload = packet.extractServiceData();

        if (stateCode != ContactServiceState.Ok.code) {
            ModuleError error = new ModuleError(NAME, stateCode);

            if (null != this.signInHandler) {
                this.signInHandler.handleFailure(this, error);
            }

            ObservableEvent event = new ObservableEvent(ContactServiceEvent.Fault, error);
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

        StableCompletionHandler completion = new StableCompletionHandler() {
            @Override
            public void handleCompletion(Module module) {
                if (gotAppendix.value && gotZoneList.value && gotGroups.value && gotBlockList.value) {
                    fireSignInCompleted();
                }
            }
        };

        long now = System.currentTimeMillis();

        // 获取附录
        this.getAppendix(this.self, new StableContactAppendixHandler() {
            @Override
            public void handleAppendix(Contact contact, ContactAppendix appendix) {
                LogUtils.d(TAG, "#getAppendix");
                gotAppendix.value = true;
                completion.handleCompletion(null);
            }
        }, new StableFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                LogUtils.d(TAG, "#getAppendix error : " + error.code);
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

                for (ContactZone contactZone : list) {
                    if (contactZone.name.equals(defaultContactZoneName)) {
                        if (null != defaultContactZone) {
                            // 重新加载默认分区
                            defaultContactZone = contactZone;
                            execute(() -> {
                                fillContactZone(defaultContactZone);
                            });
                        }
                    }
                    else if (contactZone.name.equals(defaultGroupZoneName)) {
                        if (null != defaultGroupZone) {
                            // 重新加载默认分区
                            defaultGroupZone = contactZone;
                            execute(() -> {
                                fillContactZone(defaultGroupZone);
                            });
                        }
                    }
                }

                gotZoneList.value = true;
                completion.handleCompletion(null);
            }
        });

        // 更新群组列表
        // 计算起始时间
        long beginning = this.storage.queryLastGroupActiveTime();
        if (beginning == 0) {
            beginning = now - this.retrospectDuration;
        }
        this.listGroups(beginning, now, new DefaultCompletionHandler(false) {
            @Override
            public void handleCompletion(Module module) {
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

    protected void triggerListGroups(Packet packet) {
        try {
            JSONObject data = packet.extractServiceData();

            int total = data.getInt("total");

            if (LogUtils.isDebugLevel()) {
                LogUtils.d(TAG, "List groups total: " + total);
            }

            if (null != this.workingGroupListHandler) {
                this.workingGroupListHandler.setTotal(total);
            }

            JSONArray array = data.getJSONArray("list");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject current = array.getJSONObject(i);
                Group group = new Group(current);

                // 保存到数据库
                storage.writeGroup(group);

                if (null != this.workingGroupListHandler) {
                    // 添加待处理群组
                    this.workingGroupListHandler.addGroup(group);

                    this.getAppendix(group, new StableGroupAppendixHandler() {
                        @Override
                        public void handleAppendix(Group group, GroupAppendix appendix) {
                            workingGroupListHandler.handleAppendix(group, appendix);
                        }
                    }, new StableFailureHandler() {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            workingGroupListHandler.handleFailure(module, error);
                        }
                    });
                }
            }

            if (null != this.workingGroupListHandler) {
                this.workingGroupListHandler.firePageLoaded();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void triggerCreateGroup(Packet packet) {
        try {
            Group group = new Group(packet.extractServiceData());
            // 写入数据库
            this.storage.writeGroup(group);

            this.getAppendix(group, new StableGroupAppendixHandler() {
                @Override
                public void handleAppendix(Group group, GroupAppendix appendix) {
                    // 更新到缓存
                    cache.put(group.id, group);

                    if (!group.isFilled()) {
                        fillGroup(group);
                    }

                    ObservableEvent event = new ObservableEvent(ContactServiceEvent.GroupCreated, group);
                    notifyObservers(event);
                }
            }, new StableFailureHandler() {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    ModuleError fault = new ModuleError(NAME, error.code, "Get group appendix failed");
                    fault.data = group;
                    ObservableEvent event = new ObservableEvent(ContactServiceEvent.Fault, fault);
                    notifyObservers(event);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void triggerDismissGroup(Packet packet) {
        try {
            Group group = new Group(packet.extractServiceData());

            AbstractContact abstractGroup = this.cache.get(group.id);
            if (null != abstractGroup) {
                Group current = (Group) abstractGroup;
                current.update(group);
                group = current;
            }

            // 更新
            this.storage.updateGroupProperty(group);

            if (!group.isFilled()) {
                fillGroup(group);
            }

            ObservableEvent event = new ObservableEvent(ContactServiceEvent.GroupDismissed, group);
            notifyObservers(event);
        } catch (JSONException e) {
            LogUtils.w(TAG, "#triggerDismissGroup", e);
        }
    }

    protected void triggerRemoveGroupMember(Packet packet) {
        GroupBundle bundle = new GroupBundle(packet.extractServiceData());
        Group group = bundle.group;

        boolean quit = false;
        for (Long memberId : bundle.modifiedIdList) {
            if (memberId.equals(this.self.id)) {
                quit = true;
                break;
            }
        }

        AbstractContact abstractGroup = this.cache.get(group.id);
        if (null != abstractGroup) {
            Group current = (Group) abstractGroup;
            // 更新
            current.update(group);
            for (Long memberId : bundle.modifiedIdList) {
                current.removeMember(memberId);
            }
            group = current;
        }

        if (quit) {
            // 退出群组
            group.setState(GroupState.Disabled);
        }

        // 更新数据库
        this.storage.updateGroupProperty(group);
        this.storage.removeGroupMember(bundle);

        if (quit) {
            ObservableEvent event = new ObservableEvent(ContactServiceEvent.GroupQuit, group);
            notifyObservers(event);
        }
        else {
            ObservableEvent event = new ObservableEvent(ContactServiceEvent.GroupMemberRemoved, group);
            notifyObservers(event);
        }
    }

    protected void triggerModifyZoneParticipant(Packet packet) {
        try {
            ContactZoneBundle bundle = new ContactZoneBundle(packet.extractServiceData());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void triggerModifyZone(Packet packet) {
        try {
            ContactZoneBundle bundle = new ContactZoneBundle(packet.extractServiceData());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
