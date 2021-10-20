/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Shixin Cube Team.
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

import org.json.JSONException;
import org.json.JSONObject;

import cube.auth.AuthToken;
import cube.contact.handler.SignHandler;
import cube.contact.model.Contact;
import cube.contact.model.Self;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.Packet;
import cube.util.ObservableEvent;

/**
 * 联系人模块。
 */
public class ContactService extends Module {

    /**
     * 模块名。
     */
    public final static String NAME = "Contact";

    private ContactStorage storage;

    private ContactPipelineListener pipelineListener;

    protected boolean selfReady;

    private SignHandler signInHandler;

    protected Self self;

    public ContactService() {
        super(NAME);
        this.selfReady = false;
    }

    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }

        this.storage = new ContactStorage();

        this.pipelineListener = new ContactPipelineListener(this);
        this.pipeline.addListener(NAME, this.pipelineListener);

        return true;
    }

    @Override
    public void stop() {
        super.stop();

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
        return false;
    }

    /**
     * 签入。
     *
     * @param self
     * @param handler
     * @return 如果返回 {@code false} 表示当前状态下不能进行该操作，请检查是否正确启动魔方。
     */
    public boolean signIn(Self self, SignHandler handler) {
        if (this.selfReady) {
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
                    // 更新联系人的附加上下文
                    this.storage.updateContactContext(this.self.id, this.self.getContext());
                }
                else {
                    this.self.setContext(contact.getContext());
                }

                this.execute(new Runnable() {
                    @Override
                    public void run() {
                        handler.handleSuccess(ContactService.this, ContactService.this.self);

                        ObservableEvent event = new ObservableEvent(ContactServiceEvent.SelfReady, ContactService.this.self);
                        notifyObservers(event);
                    }
                });
            }
            else {
                this.execute(new Runnable() {
                    @Override
                    public void run() {
                        handler.handleFailure(ContactService.this,
                                new ModuleError(ContactService.NAME, ContactServiceState.NoNetwork.code));
                    }
                });
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
            // 设置回调
            this.signInHandler = handler;

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
            this.execute(new Runnable() {
                @Override
                public void run() {
                    handler.handleFailure(ContactService.this,
                            new ModuleError(ContactService.NAME, ContactServiceState.InconsistentToken.code));
                }
            });
        }

        return true;
    }

    private void fireSignInCompleted() {
        // 写入数据库
        this.storage.writeContact(this.self);

        this.selfReady = true;

        this.execute(new Runnable() {
            @Override
            public void run() {
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

        fireSignInCompleted();
    }

    protected void triggerSignOut() {
        // TODO
    }
}
