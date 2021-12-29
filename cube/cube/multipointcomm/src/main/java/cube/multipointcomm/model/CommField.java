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
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import cube.contact.model.AbstractContact;
import cube.contact.model.Contact;
import cube.contact.model.Device;
import cube.contact.model.Group;
import cube.contact.model.Self;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.Packet;
import cube.core.Pipeline;
import cube.core.PipelineState;
import cube.core.handler.FailureHandler;
import cube.core.handler.PipelineHandler;
import cube.core.handler.StableFailureHandler;
import cube.multipointcomm.MediaListener;
import cube.multipointcomm.MultipointComm;
import cube.multipointcomm.MultipointCommAction;
import cube.multipointcomm.MultipointCommState;
import cube.multipointcomm.RTCDevice;
import cube.multipointcomm.handler.AnswerHandler;
import cube.multipointcomm.handler.ApplyHandler;
import cube.multipointcomm.handler.CommFieldHandler;
import cube.multipointcomm.handler.OfferHandler;
import cube.multipointcomm.handler.SignalingHandler;
import cube.multipointcomm.util.MediaConstraint;
import cube.util.LogUtils;

/**
 * 多方通信场域。
 */
public class CommField extends AbstractContact implements RTCDevice.RTCEventListener {

    private final static String TAG = "CommField";

    private MultipointComm service;

    private Context context;

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
     * 媒体约束。
     */
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

    private List<CommFieldEndpoint> endpoints;

    private RTCDevice outboundRTC;

    private RTCDevice inboundRTC;

    private ConcurrentHashMap<Long, RTCDevice> inboundRTCMap;

    /**
     * RTC 设备对应的目标。键为 RTC 设备的 SN 。
     */
    private ConcurrentHashMap<Long, CommFieldEndpoint> rtcForEndpointMap;

    public CommField(MultipointComm service, Context context, Self self, Pipeline pipeline) {
        super(self.id, self.getName() + "#" + self.id);
        this.service = service;
        this.context = context;
        this.self = self;
        this.pipeline = pipeline;
        this.founder = self;
        this.mediaConstraint = new MediaConstraint(true, true);

        this.endpoints = new ArrayList<>();
        this.inboundRTCMap = new ConcurrentHashMap<>();
        this.rtcForEndpointMap = new ConcurrentHashMap<>();
    }

    public CommField(Long id, MultipointComm service, Context context, Self self, Pipeline pipeline,
                     Group group, MediaConstraint mediaConstraint) {
        super(id, self.getName() + "#" + id);
        this.service = service;
        this.context = context;
        this.self = self;
        this.pipeline = pipeline;
        this.group = group;
        this.founder = self;
        this.mediaConstraint = mediaConstraint;

        this.endpoints = new ArrayList<>();
        this.inboundRTCMap = new ConcurrentHashMap<>();
        this.rtcForEndpointMap = new ConcurrentHashMap<>();
    }

    public CommField(JSONObject json) throws JSONException {
        super(json);
        this.founder = new Contact(json.getJSONObject("founder"));
        this.mediaConstraint = new MediaConstraint(json.getJSONObject("mediaConstraint"));
        this.startTime = json.getLong("startTime");
        this.endTime = json.getLong("endTime");

        this.endpoints = new ArrayList<>();
        if (json.has("endpoints")) {
            JSONArray array = json.getJSONArray("endpoints");
            for (int i = 0; i < array.length(); ++i) {
                CommFieldEndpoint endpoint = new CommFieldEndpoint(array.getJSONObject(i));
                endpoint.field = this;
                this.endpoints.add(endpoint);
            }
        }

        if (json.has("group")) {
            this.group = new Group(json.getJSONObject("group"));
        }

        if (json.has("caller")) {
            this.caller = new Contact(json.getJSONObject("caller"));
        }
        if (json.has("callee")) {
            this.callee = new Contact(json.getJSONObject("callee"));
        }
    }

    public void update(CommField source) {
        if (!source.endpoints.isEmpty()) {
            for (CommFieldEndpoint endpoint : source.endpoints) {
                endpoint.field = this;
            }
            this.endpoints.clear();
            this.endpoints.addAll(source.endpoints);
        }

        this.mediaConstraint = source.mediaConstraint;
        this.startTime = source.startTime;
        this.endTime = source.endTime;

        if (null == this.group && null != source.group) {
            this.group = source.group;
        }

        if (null == this.caller && null != source.caller) {
            this.caller = source.caller;
        }
        if (null == this.callee && null != source.callee) {
            this.callee = source.callee;
        }
    }

    public void assigns(MultipointComm service, Context context, Self self, Pipeline pipeline) {
        this.service = service;
        this.context = context;
        this.self = self;
        this.pipeline = pipeline;

        if (null == this.endpoints) {
            this.endpoints = new ArrayList<>();
        }

        if (null == this.inboundRTCMap) {
            this.inboundRTCMap = new ConcurrentHashMap<>();
        }

        if (null == this.rtcForEndpointMap) {
            this.rtcForEndpointMap = new ConcurrentHashMap<>();
        }
    }

    public Self getSelf() {
        return this.self;
    }

    public Contact getFounder() {
        return this.founder;
    }

    public void setFounder(Contact contact) {
        this.founder = contact;
    }

    public boolean isPrivate() {
        return this.id.longValue() == this.founder.id.longValue();
    }

    public void setMediaConstraint(MediaConstraint mediaConstraint) {
        this.mediaConstraint = mediaConstraint;
    }

    public void setCallRole(Contact caller, Contact callee) {
        this.caller = caller;
        this.callee = callee;
    }

    public Contact getCaller() {
        return this.caller;
    }

    public void setCaller(Contact caller) {
        this.caller = caller;
    }

    public Contact getCallee() {
        return this.callee;
    }

    public void setCallee(Contact callee) {
        this.callee = callee;
    }

    public Group getGroup() {
        return this.group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public MediaConstraint getMediaConstraint() {
        return this.mediaConstraint;
    }

    public int numRTCDevices() {
        int result = null != this.outboundRTC ? 1 : 0;
        return result + this.inboundRTCMap.size();
    }

    /**
     * 获取本地 RTC 设备。
     *
     * @return 返回本地 RTC 设备。
     */
    public RTCDevice getLocalDevice() {
        return this.outboundRTC;
    }

    public RTCDevice getRTCDevice() {
        return this.outboundRTC;
    }

    public RTCDevice getRTCDevice(CommFieldEndpoint endpoint) {
        RTCDevice device = this.inboundRTCMap.get(endpoint.id);
        if (null != device) {
            return device;
        }

        if (endpoint.getContact().id.longValue() == this.self.id.longValue() &&
            endpoint.getDevice().name.equals(this.self.device.name)) {
            // loopback 的 Peer
            return this.outboundRTC;
        }

        return null;
    }

    public RTCDevice getRTCDevice(long deviceSN) {
        if (null != this.outboundRTC && this.outboundRTC.getSN() == deviceSN) {
            return this.outboundRTC;
        }
        else if (null != this.inboundRTC && this.inboundRTC.getSN() == deviceSN) {
            return this.inboundRTC;
        }
        else {
            for (RTCDevice device : this.inboundRTCMap.values()) {
                if (device.getSN() == deviceSN) {
                    return device;
                }
            }

            return null;
        }
    }

    /**
     * 申请发起通话。
     *
     * @param participant
     * @param device
     * @param successHandler
     * @param failureHandler
     */
    public void applyCall(Contact participant, Device device, ApplyHandler successHandler, FailureHandler failureHandler) {
        if (!this.pipeline.isReady()) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.NoPipeline.code);
            this.service.execute(failureHandler, error);
            return;
        }

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
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(MultipointComm.NAME, packet.state.code);
                    service.execute(failureHandler, error);
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != MultipointCommState.Ok.code) {
                    ModuleError error = new ModuleError(MultipointComm.NAME, stateCode);
                    service.execute(failureHandler, error);
                    return;
                }

                JSONObject data = packet.extractServiceData();
                try {
                    CommField response = new CommField(data);
                    // 更新数据
                    update(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                service.execute(() -> {
                    successHandler.handleApply(CommField.this, participant, device);
                });
            }
        });
    }

    /**
     * 申请加入场域。
     *
     * @param participant
     * @param device
     * @param successHandler
     * @param failureHandler
     */
    public void applyJoin(Contact participant, Device device, ApplyHandler successHandler, FailureHandler failureHandler) {
        if (!this.pipeline.isReady()) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.NoPipeline.code);
            this.service.execute(failureHandler, error);
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("field", this.toCompactJSON());
            payload.put("participant", participant.toCompactJSON());
            payload.put("device", device.toCompactJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(MultipointCommAction.ApplyJoin, payload);
        this.pipeline.send(MultipointComm.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(MultipointComm.NAME, packet.state.code);
                    service.execute(failureHandler, error);
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != MultipointCommState.Ok.code) {
                    ModuleError error = new ModuleError(MultipointComm.NAME, stateCode);
                    service.execute(failureHandler, error);
                    return;
                }

                JSONObject data = packet.extractServiceData();
                try {
                    CommField response = new CommField(data);
                    // 更新数据
                    update(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                service.execute(() -> {
                    successHandler.handleApply(CommField.this, participant, device);
                });
            }
        });
    }

    /**
     * 申请终止指定参与者的数据。
     *
     * @param participant
     * @param device
     * @param successHandler
     * @param failureHandler
     */
    public void applyTerminate(Contact participant, Device device, ApplyHandler successHandler, FailureHandler failureHandler) {
        if (!this.pipeline.isReady()) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.NoPipeline.code);
            this.service.execute(failureHandler, error);
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("field", this.toCompactJSON());
            payload.put("participant", participant.toCompactJSON());
            payload.put("device", device.toCompactJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(MultipointCommAction.ApplyTerminate, payload);
        this.pipeline.send(MultipointComm.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(MultipointComm.NAME, packet.state.code);
                    service.execute(failureHandler, error);
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != MultipointCommState.Ok.code) {
                    ModuleError error = new ModuleError(MultipointComm.NAME, stateCode);
                    service.execute(failureHandler, error);
                    return;
                }

                JSONObject data = packet.extractServiceData();
                try {
                    CommField response = new CommField(data);
                    // 更新数据
                    update(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                service.execute(() -> {
                    successHandler.handleApply(CommField.this, participant, device);
                });
            }
        });
    }

    /**
     * 判断当前签入的终端是否参与了通讯。
     *
     * @return 返回 {@code true} 表示当前签入的设备已经在当前场域内。
     */
    public boolean hasJoin() {
        for (CommFieldEndpoint ep : this.endpoints) {
            if (ep.getContact().id.longValue() == this.self.id.longValue()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 终端节点数量。
     *
     * @return 返回终端节点数量。
     */
    public int numEndpoints() {
        return this.endpoints.size();
    }

    /**
     * 获取终端节点列表。
     *
     * @return 返回终端节点列表。
     */
    public List<CommFieldEndpoint> getEndpoints() {
        return new ArrayList<>(this.endpoints);
    }

    /**
     * 返回指定的终端节点的实例。
     *
     * @param contact
     * @return
     */
    public CommFieldEndpoint getEndpoint(Contact contact) {
        for (CommFieldEndpoint endpoint : this.endpoints) {
            if (endpoint.getContact().id.longValue() == contact.id.longValue()) {
                if (null == endpoint.rtcDevice) {
                    endpoint.rtcDevice = this.getRTCDevice(endpoint);
                }
                return endpoint;
            }
        }

        return null;
    }

    /**
     * 关闭指定的终端。
     *
     * @param endpoint
     */
    public void closeEndpoint(CommFieldEndpoint endpoint) {
        for (CommFieldEndpoint ep : this.endpoints) {
            if (ep.getId().longValue() == endpoint.id.longValue()) {
                // 关闭设备
                this.closeRTCDevice(ep);
                break;
            }
        }
    }

    /**
     * 启动 Offer 流程。
     *
     * @param rtcDevice
     * @param mediaConstraint
     * @param successHandler
     * @param failureHandler
     */
    public void launchOffer(RTCDevice rtcDevice, MediaConstraint mediaConstraint,
                            CommFieldHandler successHandler, FailureHandler failureHandler) {
        this.launchOffer(rtcDevice, mediaConstraint, null, successHandler, failureHandler);
    }

    /**
     * 启动 Offer 流程。
     *
     * @param rtcDevice
     * @param mediaConstraint
     * @param target
     * @param successHandler
     * @param failureHandler
     */
    public void launchOffer(RTCDevice rtcDevice, MediaConstraint mediaConstraint,
                            CommFieldEndpoint target, CommFieldHandler successHandler, FailureHandler failureHandler) {
        if (rtcDevice.getMode().equals(RTCDevice.MODE_BIDIRECTION)) {
            this.outboundRTC = rtcDevice;
        }
        else if (rtcDevice.getMode().equals(RTCDevice.MODE_SEND_ONLY)) {
            this.outboundRTC = rtcDevice;
        }
        else {
            if (null != target) {
                this.inboundRTCMap.put(target.id, rtcDevice);
            }
            else {
                this.inboundRTC = rtcDevice;
            }
        }

        if (null != target) {
            this.rtcForEndpointMap.put(rtcDevice.getSN(), target);
        }

        // 设置监听器
        rtcDevice.setEventListener(this);

        if (!this.isPrivate()) {
            // 调整为限制模式的媒体约束
            mediaConstraint.limitPattern = true;
        }

        this.service.executeOnMainThread(() -> {
            // 启用 Offer
            rtcDevice.openOffer(mediaConstraint, new OfferHandler() {
                @Override
                public void handleOffer(RTCDevice device, SessionDescription sessionDescription) {
                    // 创建信令
                    Signaling signaling = new Signaling(rtcDevice.getSN(), MultipointCommAction.Offer,
                            CommField.this, self, self.device);
                    // 设置 SDP 信息
                    signaling.sessionDescription = sessionDescription;
                    // 设置媒体约束
                    signaling.mediaConstraint = mediaConstraint;
                    // 设置目标
                    if (null != target) {
                        signaling.target = target;
                    }

                    // 发送信令
                    sendSignaling(signaling, new SignalingHandler() {
                        @Override
                        public void handleSignaling(Signaling signaling) {
                            // 更新数据
                            update(signaling.field);
                            // 回调
                            service.execute(() -> {
                                successHandler.handleCommField(CommField.this);
                            });
                        }
                    }, new StableFailureHandler() {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            service.execute(() -> {
                                failureHandler.handleFailure(service, error);
                            });
                        }
                    });
                }
            }, new StableFailureHandler() {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    service.execute(() -> {
                        failureHandler.handleFailure(service, error);
                    });
                }
            });
        });
    }

    /**
     * 启动为 Answer 。
     *
     * @param rtcDevice
     * @param sessionDescription
     * @param mediaConstraint
     * @param successHandler
     * @param failureHandler
     */
    public void launchAnswer(RTCDevice rtcDevice, SessionDescription sessionDescription, MediaConstraint mediaConstraint,
                             CommFieldHandler successHandler, FailureHandler failureHandler) {
        this.launchAnswer(rtcDevice, sessionDescription, mediaConstraint, null, successHandler, failureHandler);
    }

    /**
     * 启动为 Answer 。
     *
     * @param rtcDevice
     * @param sessionDescription
     * @param mediaConstraint
     * @param target
     * @param successHandler
     * @param failureHandler
     */
    public void launchAnswer(RTCDevice rtcDevice, SessionDescription sessionDescription, MediaConstraint mediaConstraint,
                             CommFieldEndpoint target, CommFieldHandler successHandler, FailureHandler failureHandler) {
        if (rtcDevice.getMode().equals(RTCDevice.MODE_BIDIRECTION) || rtcDevice.getMode().equals(RTCDevice.MODE_SEND_ONLY)) {
            this.outboundRTC = rtcDevice;
        }
        else {
            if (null != target) {
                this.inboundRTCMap.put(target.id, rtcDevice);
            }
            else {
                this.inboundRTC = rtcDevice;
            }
        }

        // 设置监听器
        rtcDevice.setEventListener(this);

        this.service.executeOnMainThread(() -> {
            // 启用 Answer
            rtcDevice.openAnswer(sessionDescription, mediaConstraint, new AnswerHandler() {
                @Override
                public void handleAnswer(RTCDevice device, SessionDescription sessionDescription) {
                    // 创建信令
                    Signaling signaling = new Signaling(rtcDevice.getSN(), MultipointCommAction.Answer,
                            CommField.this, self, self.device);
                    // 设置 SDP 信息
                    signaling.sessionDescription = sessionDescription;
                    // 设置媒体约束
                    signaling.mediaConstraint = mediaConstraint;

                    // 设置目标
                    if (null != target) {
                        signaling.target = target;
                    }

                    // 发送信令
                    sendSignaling(signaling, new SignalingHandler() {
                        @Override
                        public void handleSignaling(Signaling signaling) {
                            // 更新数据
                            update(signaling.field);
                            // 回调
                            service.execute(() -> {
                                successHandler.handleCommField(CommField.this);
                            });
                        }
                    }, new StableFailureHandler() {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            service.execute(() -> {
                                failureHandler.handleFailure(service, error);
                            });
                        }
                    });
                }
            }, new StableFailureHandler() {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    service.execute(() -> {
                        failureHandler.handleFailure(service, error);
                    });
                }
            });
        });
    }

    private void sendSignaling(Signaling signaling, SignalingHandler successHandler, FailureHandler failureHandler) {
        if (!this.pipeline.isReady()) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.NoPipeline.code);
            this.service.execute(failureHandler, error);
            return;
        }

        this.pipeline.send(MultipointComm.NAME, new Packet(signaling.name, signaling.toJSON()), new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(MultipointComm.NAME, packet.state.code);
                    service.execute(failureHandler, error);
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != MultipointCommState.Ok.code) {
                    ModuleError error = new ModuleError(MultipointComm.NAME, stateCode);
                    service.execute(failureHandler, error);
                    return;
                }

                try {
                    Signaling response = new Signaling(packet.extractServiceData());
                    successHandler.handleSignaling(response);
                } catch (JSONException e) {
                    LogUtils.w(TAG, "#sendSignaling", e);
                }
            }
        });
    }

    public void close() {
        this.closeRTCDevices();
        this.endpoints.clear();

        this.caller = null;
        this.callee = null;
        this.group = null;
    }

    private void closeRTCDevices() {
        if (null != this.outboundRTC) {
            this.outboundRTC.close();
            this.outboundRTC = null;
        }

        for (RTCDevice rtcDevice : this.inboundRTCMap.values()) {
            rtcDevice.close();
        }
        this.inboundRTCMap.clear();

        this.rtcForEndpointMap.clear();

        if (null != this.inboundRTC) {
            this.inboundRTC.close();
            this.inboundRTC = null;
        }
    }

    public void closeRTCDevice(CommFieldEndpoint endpoint) {
        if (null != this.outboundRTC) {
            if (this.self.id.longValue() == endpoint.getContact().id.longValue() &&
                this.self.device.name.equals(endpoint.getDevice().name)) {
                this.outboundRTC.close();
                this.outboundRTC = null;
                return;
            }
        }

        RTCDevice rtcDevice = this.inboundRTCMap.remove(endpoint.id);
        if (null != rtcDevice) {
            this.rtcForEndpointMap.remove(rtcDevice.getSN());
            rtcDevice.close();
        }
    }

    /**
     * 是否已经存在指定终端的 RTC 设备。
     *
     * @param endpoint
     * @return
     */
    public boolean hasRTCDevice(CommFieldEndpoint endpoint) {
        return this.inboundRTCMap.containsKey(endpoint.id);
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate, RTCDevice rtcDevice) {
        if (LogUtils.isDebugLevel()) {
            LogUtils.d(TAG, "#onIceCandidate");
        }

        Signaling signaling = new Signaling(rtcDevice.getSN(), MultipointCommAction.Candidate, this,
                this.getSelf(), this.getSelf().device);
        // 设置 Candidate
        signaling.candidate = iceCandidate;

        CommFieldEndpoint endpoint = this.rtcForEndpointMap.get(rtcDevice.getSN());
        if (null != endpoint) {
            signaling.target = endpoint;
        }

        this.sendSignaling(signaling, new SignalingHandler() {
            @Override
            public void handleSignaling(Signaling signaling) {
                // Nothing
            }
        }, new StableFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                // Nothing
            }
        });
    }

    public void setMediaListener(MediaListener mediaListener) {
        this.mediaListener = mediaListener;
    }

    @Override
    public void onMediaConnected(RTCDevice rtcDevice) {
        if (null != this.mediaListener) {
            this.mediaListener.onMediaConnected(this, rtcDevice);
        }
    }

    @Override
    public void onMediaDisconnected(RTCDevice rtcDevice) {
        if (null != this.mediaListener) {
            this.mediaListener.onMediaDisconnected(this, rtcDevice);
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
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
