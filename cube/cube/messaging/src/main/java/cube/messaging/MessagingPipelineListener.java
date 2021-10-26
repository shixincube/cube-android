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

import cube.core.Packet;
import cube.core.Pipeline;
import cube.core.PipelineListener;
import cube.core.PipelineState;

/**
 * 消息模块数据通道监听器。
 */
public class MessagingPipelineListener implements PipelineListener {

    private MessagingService service;

    public MessagingPipelineListener(MessagingService service) {
        this.service = service;
    }

    @Override
    public void onReceived(Pipeline pipeline, String source, Packet packet) {
        if (packet.state.code != PipelineState.Ok.code) {
            Log.d("MessagingPipelineListener", "#onReceived code : " + packet.state.code);
            return;
        }

        if (MessagingAction.Notify.equals(packet.name)) {
            this.service.triggerNotify(packet.extractServiceData());
        }
        else if (MessagingAction.Pull.equals(packet.name)) {
            this.service.triggerPull(packet.extractServiceStateCode(), packet.extractServiceData());
        }
    }

    @Override
    public void onOpened(Pipeline pipeline) {
        // Nothing
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
