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

package cube.pipelline;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cell.api.Nucleus;
import cell.api.NucleusConfig;
import cell.api.Speakable;
import cell.api.TalkListener;
import cell.core.talk.Primitive;
import cell.core.talk.TalkError;
import cell.core.talk.dialect.ActionDialect;
import cube.core.Packet;
import cube.core.Pipeline;
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

    private String tokenCode;

    public CellPipeline(Context context) {
        super();
        this.opening = false;
        this.enabled = false;

        NucleusConfig config = new NucleusConfig();
        this.nucleus = new Nucleus(context, config);
        this.nucleus.getTalkService().addListener(this);

        this.responseCallbackMap = new ConcurrentHashMap<>();
    }

    public void setTokenCode(String tokenCode) {
        this.tokenCode = tokenCode;
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
    public void send(String destination, Packet packet) {
        ActionDialect dialect = this.convertPacketToPrimitive(packet);
        this.nucleus.getTalkService().speak(destination, dialect);
    }

    @Override
    public void send(String destination, Packet packet, PipelineHandler handler) {
        long timestamp = System.currentTimeMillis();
        this.responseCallbackMap.put(packet.sn, new ResponseCallback(destination, handler, timestamp));

        ActionDialect dialect = this.convertPacketToPrimitive(packet);
        this.nucleus.getTalkService().speak(destination, dialect);
    }

    @Override
    public void onListened(Speakable speakable, String s, Primitive primitive) {

    }

    @Override
    public void onSpoke(Speakable speakable, String s, Primitive primitive) {

    }

    @Override
    public void onAck(Speakable speakable, String s, Primitive primitive) {

    }

    @Override
    public void onSpeakTimeout(Speakable speakable, String s, Primitive primitive) {

    }

    @Override
    public void onContacted(Speakable speakable) {

    }

    @Override
    public void onQuitted(Speakable speakable) {

    }

    @Override
    public void onFailed(Speakable speakable, TalkError talkError) {

    }

    private ActionDialect convertPacketToPrimitive(Packet packet) {
        ActionDialect dialect = new ActionDialect(packet.name);
        dialect.addParam("sn", packet.sn.longValue());
        dialect.addParam("data", packet.getData());
        if (null != this.tokenCode) {
            dialect.addParam("token", this.tokenCode);
        }
        return dialect;
    }

    private Packet convertPrimitiveToPacket(ActionDialect dialect) {
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
