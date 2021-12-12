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

package cube.multipointcomm.model;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import cube.auth.AuthService;
import cube.contact.model.Contact;
import cube.contact.model.Device;
import cube.contact.model.Group;
import cube.contact.model.Self;
import cube.core.Packet;
import cube.core.Pipeline;
import cube.core.handler.FailureHandler;
import cube.core.handler.PipelineHandler;
import cube.core.model.Entity;
import cube.multipointcomm.MediaListener;
import cube.multipointcomm.MultipointComm;
import cube.multipointcomm.MultipointCommAction;
import cube.multipointcomm.RTCDevice;
import cube.multipointcomm.handler.CommFieldHandler;
import cube.multipointcomm.util.MediaConstraint;

/**
 * 多方通信场域。
 */
public class CommField extends Entity {

    private Self self;

    /**
     * 通信管道。
     */
    private Pipeline pipeline;

    /**
     * 通信场创建人。
     */
    private Contact founder;

    /**
     * 场域名称。
     */
    private String name;

    private MediaConstraint mediaConstraint;

    /**
     * 通讯域开始通话时间。
     */
    private long startTime;

    /**
     * 通讯域结束通话时间。
     */
    private long endTime;

    /**
     * 主叫。
     */
    private Contact caller;

    /**
     * 被叫。
     */
    private Contact callee;

    /**
     * 包含的群组。
     */
    private Group group;

    /**
     * 媒体监听器。
     */
    private MediaListener mediaListener;

    private Context context;

    private EglBase eglBase;

    private List<CommFieldEndpoint> endpoints;

    private RTCDevice outboundRTC;

    private ConcurrentHashMap<Long, RTCDevice> inboundRTCMap;

    public CommField(Context context, Self self, Pipeline pipeline) {
        super(self.id);
        this.context = context;
        this.self = self;
        this.pipeline = pipeline;
        this.founder = self;
        this.name = this.founder.getName() + "#" + this.id;
        this.mediaConstraint = new MediaConstraint(true, true);
        this.eglBase = EglBase.create();
        this.endpoints = new ArrayList<>();
        this.inboundRTCMap = new ConcurrentHashMap<>();
    }

    public Self getSelf() {
        return this.self;
    }

    public Contact getFounder() {
        return this.founder;
    }

    public void setMediaConstraint(MediaConstraint mediaConstraint) {
        this.mediaConstraint = mediaConstraint;
    }

    public void setCallRole(Contact caller, Contact callee) {
        this.caller = caller;
        this.callee = callee;
    }

    public int numRTCDevices() {
        int result = null != this.outboundRTC ? 1 : 0;
        return result + this.inboundRTCMap.size();
    }

    public void applyCall(Contact participant, Device device, CommFieldHandler successHandler, FailureHandler failureHandler) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("field", this.toCompactJSON());
            payload.put("participant", participant.toCompactJSON());
            payload.put("device", device.toCompactJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(MultipointCommAction.ApplyCall, payload);
        this.pipeline.send(MultipointComm.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {

            }
        });
    }

    private PeerConnectionFactory createPeerConnectionFactory() {
        PeerConnectionFactory.InitializationOptions initOptions = PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions();
        PeerConnectionFactory.initialize(initOptions);

        // 编码器
        VideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true);
        // 解码器
        VideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.disableNetworkMonitor = true;

        // Audio Device module
        JavaAudioDeviceModule audioDeviceModule = JavaAudioDeviceModule.builder(this.context)
                .setSamplesReadyCallback(null)
                .setUseHardwareAcousticEchoCanceler(false)
                .setUseHardwareNoiseSuppressor(false)
                .setAudioRecordErrorCallback(null)
                .setAudioTrackErrorCallback(null)
                .createAudioDeviceModule();

        PeerConnectionFactory factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(audioDeviceModule)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();

        return factory;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("domain", AuthService.getDomain());
            json.put("name", this.name);
            json.put("founder", this.founder.toCompactJSON());
            json.put("mediaConstraint", this.mediaConstraint.toJSON());
            json.put("startTime", this.startTime);
            json.put("endTime", this.endTime);

            JSONArray endpointArray = new JSONArray();
            for (CommFieldEndpoint endpoint : this.endpoints) {
                endpointArray.put(endpoint.toJSON());
            }
            json.put("endpoints", endpointArray);

            if (null != this.group) {
                json.put("group", this.group.toCompactJSON());
            }

            if (null != this.caller) {
                json.put("caller", this.caller.toCompactJSON());
            }
            if (null != this.callee) {
                json.put("callee", this.callee.toCompactJSON());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        try {
            json.put("domain", AuthService.getDomain());
            json.put("name", this.name);
            json.put("founder", this.founder.toCompactJSON());
            json.put("mediaConstraint", this.mediaConstraint.toJSON());
            json.put("startTime", this.startTime);
            json.put("endTime", this.endTime);

            if (null != this.group) {
                json.put("group", this.group.toCompactJSON());
            }

            if (null != this.caller) {
                json.put("caller", this.caller.toCompactJSON());
            }
            if (null != this.callee) {
                json.put("callee", this.callee.toCompactJSON());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
