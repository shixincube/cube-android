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

import cube.core.Packet;
import cube.core.Pipeline;
import cube.core.PipelineListener;
import cube.core.PipelineState;

/**
 * 联系模块数据通道监听器。
 */
public class ContactPipelineListener implements PipelineListener {

    private ContactService service;

    public ContactPipelineListener(ContactService service) {
        this.service = service;
    }

    @Override
    public void onReceived(Pipeline pipeline, String source, Packet packet) {
        if (packet.state.code != PipelineState.Ok.code) {
            Log.e("ContactPipelineListener", "#onReceived : " + packet.state.code);
            return;
        }

        if (ContactServiceAction.SignIn.equals(packet.name)) {
            this.service.triggerSignIn(packet.extractServiceStateCode(), packet.extractServiceData());
        }
        else if (ContactServiceAction.ListGroups.equals(packet.name)) {
            this.service.triggerListGroups(packet.extractServiceData());
        }
        else if (ContactServiceAction.SignOut.equals(packet.name)) {
            this.service.triggerSignOut();
        }
    }

    @Override
    public void onOpened(Pipeline pipeline) {
        // 如果用户请求签入但是签入失败（在签入时可能没有网络），则在连接建立后尝试自动签入
        if (null != this.service.self && !this.service.signInReady.get()) {
            (new Thread() {
                @Override
                public void run() {
                    service.signIn(service.self, null);
                }
            }).start();
        }
    }

    @Override
    public void onClosed(Pipeline pipeline) {
        // Nothing
    }

    @Override
    public void onFaultOccurred(Pipeline pipeline, int code, String description) {
        // Nothing
    }
}
