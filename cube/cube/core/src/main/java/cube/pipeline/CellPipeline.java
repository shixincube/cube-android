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

package cube.pipeline;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import cell.api.Nucleus;
import cell.api.NucleusConfig;
import cell.api.Speakable;
import cell.api.TalkListener;
import cell.core.talk.Primitive;
import cell.core.talk.TalkError;
import cell.core.talk.dialect.ActionDialect;
import cell.util.NetworkUtils;
import cube.core.Packet;
import cube.core.Pipeline;
import cube.core.PipelineListener;
import cube.core.PipelineState;
import cube.core.handler.PipelineHandler;

/**
 * 基于 Cell 的数据通道。
 */
public class CellPipeline extends Pipeline implements TalkListener {

    private boolean opening;

    private boolean enabled;

    private Nucleus nucleus;

    private Map<Long, ResponseCallback> responseCallbackMap;

    public CellPipeline(Context context) {
        super();
        this.opening = false;
        this.enabled = false;

        NucleusConfig config = new NucleusConfig();
        this.nucleus = new Nucleus(context, config);
        this.nucleus.getTalkService().addListener(this);

        this.responseCallbackMap = new ConcurrentHashMap<>();
    }

    @Override
    public void open() {
        if (this.isReady()) {
            return;
        }

        if (this.opening) {
            return;
        }

        this.opening = true;
        this.enabled = true;

        this.nucleus.getTalkService().call(this.address, this.port);
    }

    @Override
    public void close() {
        this.enabled = false;

        this.nucleus.getTalkService().hangup(this.address, this.port, true);
    }

    @Override
    public boolean isReady() {
        return this.nucleus.getTalkService().isCalled(this.address, this.port);
    }

    @Override
    public boolean send(String destination, Packet packet) {
        if (!this.isReady()) {
            return false;
        }

        ActionDialect dialect = this.convertPacketToDialect(packet);
        return this.nucleus.getTalkService().speak(destination, dialect);
    }

    @Override
    public boolean send(String destination, Packet packet, PipelineHandler handler) {
        if (!this.isReady()) {
            return false;
        }

        long timestamp = System.currentTimeMillis();
        this.responseCallbackMap.put(packet.sn, new ResponseCallback(destination, handler, timestamp));

        ActionDialect dialect = this.convertPacketToDialect(packet);
        return this.nucleus.getTalkService().speak(destination, dialect);
    }

    @Override
    public void fireNetworkStatusChanged(boolean connected) {
        if (this.opening) {
            return;
        }

        if (this.enabled && connected) {
            if (!this.isReady()) {
                // 重连
                this.retry(500, false);
            }
        }
    }

    @Override
    public void onListened(Speakable speakable, String cellet, Primitive primitive) {
        ActionDialect dialect = new ActionDialect(primitive);
        // 转为数据包格式
        Packet packet = this.convertDialectToPacket(dialect);

        ResponseCallback callback = this.responseCallbackMap.remove(packet.sn);
        if (null != callback) {
            callback.handler.handleResponse(packet);
        }

        this.triggerListener(cellet, packet);
    }

    @Override
    public void onSpoke(Speakable speakable, String cellet, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onAck(Speakable speakable, String cellet, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onSpeakTimeout(Speakable speakable, String cellet, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onContacted(Speakable speakable) {
        this.opening = false;

        List<PipelineListener> listeners = this.getAllListeners();
        for (PipelineListener listener : listeners) {
            listener.onOpened(this);
        }
    }

    @Override
    public void onQuitted(Speakable speakable) {
        this.opening = false;

        List<PipelineListener> listeners = this.getAllListeners();
        for (PipelineListener listener : listeners) {
            listener.onClosed(this);
        }
    }

    @Override
    public void onFailed(Speakable speakable, TalkError talkError) {
        List<PipelineListener> listeners = this.getAllListeners();
        for (PipelineListener listener : listeners) {
            listener.onFaultOccurred(this, talkError.getErrorCode(), "Pipeline error");
        }

        if (this.enabled) {
            if (NetworkUtils.isConnected(this.nucleus.getTag().getContext())) {
                this.retry(2000, true);
            }
        }
    }

    private void retry(long delay, boolean hangUpBefore) {
        Log.i("CellPipeline", "Retry connect : " + this.address + ":" + this.port);

        if (hangUpBefore) {
            this.nucleus.getTalkService().hangup(this.address, this.port, true);
        }

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!nucleus.getTalkService().isCalled(address, port)) {
                    nucleus.getTalkService().call(address, port);
                }
            }
        }, delay);
    }

    private ActionDialect convertPacketToDialect(Packet packet) {
        ActionDialect dialect = new ActionDialect(packet.name);
        dialect.addParam("sn", packet.sn.longValue());
        dialect.addParam("data", packet.getData());
        if (null != this.tokenCode) {
            dialect.addParam("token", this.tokenCode);
        }
        return dialect;
    }

    private Packet convertDialectToPacket(ActionDialect dialect) {
        Packet packet = new Packet(dialect.getParamAsLong("sn"), dialect.getName(), dialect.getParamAsJson("data"));
        if (dialect.containsParam("state")) {
            packet.state = extractState(dialect.getParamAsJson("state"));
        }
        return packet;
    }

    private PipelineState extractState(JSONObject json) {
        PipelineState state = PipelineState.GatewayError;
        try {
            int code = json.getInt("code");
            state = PipelineState.parse(code);
        } catch (JSONException e) {
            // Nothing
        }
        return state;
    }

    private class ResponseCallback {

        public final String destination;

        public final PipelineHandler handler;

        public final long timestamp;

        public ResponseCallback(String destination, PipelineHandler handler, long timestamp) {
            this.destination = destination;
            this.handler = handler;
            this.timestamp = timestamp;
        }
    }
}
