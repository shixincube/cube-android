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

import cube.contact.ContactService;
import cube.contact.ContactServiceEvent;
import cube.contact.model.Self;
import cube.core.Module;
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

    public MessagingService() {
        super(MessagingService.NAME);
        this.pipelineListener = new MessagingPipelineListener(this);
        this.storage = new MessagingStorage(this);
        this.observer = new MessagingObserver(this);
        this.ready = false;
    }

    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }

        this.pipeline.addListener(MessagingService.NAME, this.pipelineListener);

        // 监听联系人模块
        this.contactService = (ContactService) this.kernel.getModule(ContactService.NAME);
        this.contactService.attachWithName(ContactServiceEvent.SignIn, this.observer);
        this.contactService.attachWithName(ContactServiceEvent.SignOut, this.observer);

        synchronized (this) {
            if (null != this.contactService.getSelf() && !this.ready) {
                this.prepare(new Runnable() {
                    @Override
                    public void run() {
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

        this.contactService.detachWithName(ContactServiceEvent.SignIn, this.observer);
        this.contactService.detachWithName(ContactServiceEvent.SignOut, this.observer);

        this.pipeline.removeListener(MessagingService.NAME, this.pipelineListener);

        // 关闭存储
        this.storage.close();
    }

    @Override
    public boolean isReady() {
        return false;
    }

    private void prepare(Runnable completedHandler) {
        Self self = this.contactService.getSelf();

        // 开启存储
        this.storage.open(this.getContext(), self.id, self.domain);

        // 查询本地最近消息时间


        this.ready = true;
    }

    protected void fireContactEvent(ObservableEvent event) {
        if (event.name.equals(ContactServiceEvent.SignIn)
            || event.name.equals(ContactServiceEvent.SelfReady)) {
            synchronized (this) {
                if (!this.ready) {
                    // 准备数据
                    this.prepare(new Runnable() {
                        @Override
                        public void run() {
                            ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Ready, MessagingService.this);
                            notifyObservers(event);
                        }
                    });
                }
            }
        }
    }
}
