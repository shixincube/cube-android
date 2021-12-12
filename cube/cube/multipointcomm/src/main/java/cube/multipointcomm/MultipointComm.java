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

package cube.multipointcomm;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.Timer;

import cube.contact.ContactService;
import cube.contact.ContactServiceEvent;
import cube.contact.model.Contact;
import cube.contact.model.Self;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.FailureHandler;
import cube.multipointcomm.handler.CallHandler;
import cube.multipointcomm.model.CallRecord;
import cube.multipointcomm.model.CommField;
import cube.multipointcomm.util.MediaConstraint;
import cube.util.ObservableEvent;
import cube.util.Observer;

/**
 * 多方通信服务。
 */
public class MultipointComm extends Module implements Observer {

    public final static String NAME = "MultipointComm";

    private JSONObject configuration;

    private Timer callTimer;

    private CommField privateField;

    private CallRecord activeCall;

    public MultipointComm() {
        super(NAME);
    }

    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }

        ContactService contactService = ((ContactService) this.kernel.getModule(ContactService.NAME));
        contactService.attachWithName(ContactServiceEvent.SelfReady, this);
        Self self = contactService.getSelf();
        if (null != self) {
            this.privateField = new CommField(this.getContext(), self, this.pipeline);
        }

        return true;
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    protected void config(@Nullable JSONObject configData) {
        this.configuration = configData;
    }

    @Override
    public boolean isReady() {
        return false;
    }

    public void makeCall(Contact contact, MediaConstraint mediaConstraint, CallHandler successHandler, FailureHandler failureHandler) {
        if (null == this.privateField) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.NoCommField.code);
            execute(failureHandler, error);
            return;
        }

        if (null != this.activeCall && this.activeCall.isActive()) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.CallerBusy.code);
            execute(failureHandler, error);
            return;
        }

        execute(() -> {
            this.callTimer = new Timer();
        });

        // 创建通话记录
        this.activeCall = new CallRecord(this.privateField.getSelf(), this.privateField);

        // 设置主叫和被叫
        this.privateField.setCallRole(this.privateField.getFounder(), contact);

        // 1. 申请一对一主叫

    }

    @Override
    public void update(ObservableEvent event) {
        if (ContactServiceEvent.SelfReady.equals(event.getName())) {
            Self self = (Self) event.getData();
            if (null == this.privateField) {
                this.privateField = new CommField(this.getContext(), self, this.pipeline);
            }
        }
    }
}
