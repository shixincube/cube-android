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

package cube.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cube.core.handler.PipelineHandler;

/**
 * 数据通道服务接口。
 */
public abstract class Pipeline {

    private final static String DRIFTLESS_LISTENER = "*";

    private Map<String, List<PipelineListener>> listeners;

    protected String address;

    protected int port;

    protected String tokenCode;

    public Pipeline() {
        this.listeners = new HashMap<>();
    }

    public void setRemoteAddress(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public void setTokenCode(String tokenCode) {
        this.tokenCode = tokenCode;
    }

    public void addListener(String destination, PipelineListener listener) {
        synchronized (this) {
            List<PipelineListener> list = this.listeners.get(destination);
            if (null == list) {
                list = new ArrayList<>();
                list.add(listener);
                this.listeners.put(destination, list);
            }
            else {
                if (!list.contains(listener)) {
                    list.add(listener);
                }
            }
        }
    }

    public void addListener(PipelineListener listener) {
        this.addListener(DRIFTLESS_LISTENER, listener);
    }

    public void removeListener(String destination, PipelineListener listener) {
        synchronized (this) {
            List<PipelineListener> list = this.listeners.get(destination);
            if (null != list) {
                list.remove(listener);
            }
        }
    }

    public void removeListener(PipelineListener listener) {
        this.removeListener(DRIFTLESS_LISTENER, listener);
    }

    public List<PipelineListener> getListeners(String destination) {
        return this.listeners.get(destination);
    }

    public List<PipelineListener> getAllListeners() {
        ArrayList<PipelineListener> result = new ArrayList<>();
        for (List<PipelineListener> list : this.listeners.values()) {
            for (PipelineListener listener : list) {
                if (!result.contains(listener)) {
                    result.add(listener);
                }
            }
        }
        return result;
    }

    protected void triggerListener(String destination, Packet packet) {
        List<PipelineListener> list = this.listeners.get(destination);
        if (null != list) {
            for (PipelineListener listener : list) {
                listener.onReceived(this, destination, packet);
            }
        }
    }

    /**
     * 开启数据通道。
     */
    public abstract void open();

    /**
     * 关闭数据通道。
     */
    public abstract void close();

    /**
     * 是否就绪。
     * @return
     */
    public abstract boolean isReady();

    /**
     * 向指定目标发送数据。
     * @param destination
     * @param packet
     */
    public abstract void send(String destination, Packet packet);

    /**
     * 向指定目标发送数据。
     * @param destination
     * @param packet
     * @param handler
     */
    public abstract void send(String destination, Packet packet, PipelineHandler handler);

    /**
     * 当网络状态改变时触发。
     *
     * @param connected
     */
    public abstract void fireNetworkStatusChanged(boolean connected);
}
