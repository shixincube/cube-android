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

package cube.ferry;

import org.json.JSONException;

import cube.core.Packet;
import cube.core.Pipeline;
import cube.core.PipelineListener;
import cube.ferry.model.Tenet;
import cube.util.LogUtils;

/**
 * 摆渡模块数据通道监听器。
 */
public class FerryPipelineListener implements PipelineListener {

    private final static String TAG = FerryPipelineListener.class.getSimpleName();

    private FerryService service;

    public FerryPipelineListener(FerryService service) {
        this.service = service;
    }

    @Override
    public void onReceived(Pipeline pipeline, String source, Packet packet) {
        if (FerryServiceAction.Online.equals(packet.name)) {
            this.service.triggerOnline(packet);
        }
        else if (FerryServiceAction.Offline.equals(packet.name)) {
            this.service.triggerOffline(packet);
        }
        else if (FerryServiceAction.Tenet.equals(packet.name)) {
            try {
                Tenet tenet = this.service.createTenet(packet.extractServiceData());
                if (null != tenet) {
                    this.service.triggerTenet(tenet);
                }
                else {
                    LogUtils.e(TAG, "#onReceived - tenet is null");
                }
            } catch (JSONException e) {
                LogUtils.w(TAG, "#onReceived - " + packet.name, e);
            }
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

    }
}
