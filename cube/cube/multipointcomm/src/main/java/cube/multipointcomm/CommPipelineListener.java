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

import cube.core.Packet;
import cube.core.Pipeline;
import cube.core.PipelineListener;
import cube.core.PipelineState;
import cube.util.LogUtils;

/**
 * 通道监听器。
 */
public class CommPipelineListener implements PipelineListener {

    private MultipointComm service;

    public CommPipelineListener(MultipointComm service) {
        this.service = service;
    }

    @Override
    public void onReceived(Pipeline pipeline, String source, Packet packet) {
        if (packet.state.code != PipelineState.Ok.code) {
            LogUtils.w("CommPipelineListener", "Pipeline error: " + packet.name + " - " + packet.state.code);
            return;
        }

        if (MultipointCommAction.Offer.equals(packet.name)) {
            this.service.triggerOffer(packet);
        }
        else if (MultipointCommAction.Answer.equals(packet.name)) {
            this.service.triggerAnswer(packet);
        }
        else if (MultipointCommAction.Candidate.equals(packet.name)) {
            this.service.triggerCandidate(packet);
        }
        else if (MultipointCommAction.Bye.equals(packet.name)) {
            this.service.triggerBye(packet);
        }
        else if (MultipointCommAction.Busy.equals(packet.name)) {
            this.service.triggerBusy(packet);
        }
        else if (MultipointCommAction.Invite.equals(packet.name)) {
            this.service.triggerInvite(packet);
        }
        else if (MultipointCommAction.Arrived.equals(packet.name)) {
            this.service.triggerArrived(packet);
        }
        else if (MultipointCommAction.Left.equals(packet.name)) {
            this.service.triggerLeft(packet);
        }
    }

    @Override
    public void onOpened(Pipeline pipeline) {

    }

    @Override
    public void onClosed(Pipeline pipeline) {

    }

    @Override
    public void onFaultOccurred(Pipeline pipeline, int code, String description) {

    }
}
