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

import android.view.ViewGroup;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import cube.contact.ContactService;
import cube.contact.ContactServiceEvent;
import cube.contact.model.Contact;
import cube.contact.model.Device;
import cube.contact.model.Group;
import cube.contact.model.Self;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.Packet;
import cube.core.PipelineState;
import cube.core.handler.FailureHandler;
import cube.core.handler.PipelineHandler;
import cube.core.handler.StableFailureHandler;
import cube.multipointcomm.handler.CallHandler;
import cube.multipointcomm.handler.CommFieldHandler;
import cube.multipointcomm.handler.DefaultApplyCallHandler;
import cube.multipointcomm.handler.DefaultCallHandler;
import cube.multipointcomm.handler.DefaultCommFieldHandler;
import cube.multipointcomm.handler.RTCDeviceHandler;
import cube.multipointcomm.model.CallRecord;
import cube.multipointcomm.model.CommField;
import cube.multipointcomm.model.CommFieldEndpoint;
import cube.multipointcomm.model.Signaling;
import cube.multipointcomm.util.MediaConstraint;
import cube.util.LogUtils;
import cube.util.ObservableEvent;
import cube.util.Observer;

/**
 * 多方通信服务。
 */
public class MultipointComm extends Module implements Observer {

    private final static String TAG = MultipointComm.class.getSimpleName();

    public final static String NAME = "MultipointComm";

    private ContactService contactService;

    private CommPipelineListener pipelineListener;

    private EglBase eglBase;

    private PeerConnectionFactory peerConnectionFactory;

    private List<PeerConnection.IceServer> iceServers;

    private Timer callTimer;
    private long callTimeout = 30000;

    private CommField privateField;

    private ConcurrentHashMap<Long, CommField> commFieldMap;

    private CallRecord activeCall;

    private List<CallListener> callListeners;

    private Signaling offerSignaling;
    private Signaling answerSignaling;

    private ViewGroup localVideoContainer;
    private ViewGroup remoteVideoContainer;

    public MultipointComm() {
        super(NAME);
        this.callListeners = new ArrayList<>();
    }

    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }

        this.commFieldMap = new ConcurrentHashMap<>();

        this.pipelineListener = new CommPipelineListener(this);
        this.pipeline.addListener(MultipointComm.NAME, this.pipelineListener);

        super.executeOnMainThread(() -> {
            this.eglBase = EglBase.create();
            this.peerConnectionFactory = createPeerConnectionFactory();
        });

        this.contactService = ((ContactService) this.kernel.getModule(ContactService.NAME));
        this.contactService.attachWithName(ContactServiceEvent.SelfReady, this);
        Self self = this.contactService.getSelf();
        if (null != self) {
            this.privateField = new CommField(this, this.getContext(), self, this.pipeline);
        }

        return true;
    }

    @Override
    public void stop() {
        super.stop();

        this.callListeners.clear();

        if (null != this.pipelineListener) {
            this.pipeline.removeListener(MultipointComm.NAME, this.pipelineListener);
        }
    }

    @Override
    protected void config(@Nullable JSONObject configData) {
        this.iceServers = new ArrayList<>();
        if (configData.has("iceServers")) {
            try {
                JSONArray array = configData.getJSONArray("iceServers");
                for (int i = 0; i < array.length(); ++i) {
                    JSONObject server = array.getJSONObject(i);
                    String urls = server.getString("urls");
                    PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder(urls)
                            .setUsername(server.getString("username"))
                            .setPassword(server.getString("credential"))
                            .createIceServer();
                    this.iceServers.add(iceServer);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isReady() {
        return false;
    }

    public ContactService getContactService() {
        return this.contactService;
    }

    /**
     * 添加通话监听。
     *
     * @param listener
     */
    public void addCallListener(CallListener listener) {
        if (!this.callListeners.contains(listener)) {
            this.callListeners.add(listener);
        }
    }

    /**
     * 移除通话监听器。
     *
     * @param listener
     */
    public void removeCallListener(CallListener listener) {
        this.callListeners.remove(listener);
    }

    /**
     * 设置本地视频容器。
     *
     * @param container
     */
    public void setLocalVideoContainer(ViewGroup container) {
        this.localVideoContainer = container;
    }

    /**
     * 设置远端视频容器。
     *
     * @param container
     */
    public void setRemoteVideoContainer(ViewGroup container) {
        this.remoteVideoContainer = container;
    }

    /**
     * 获取当前正在活跃的通话记录。
     *
     * @return 返回当前正在活跃的通话记录。
     */
    public CallRecord getActiveCallRecord() {
        return this.activeCall;
    }

    /**
     * 发起通话。
     *
     * @param contact
     * @param mediaConstraint
     * @param successHandler
     * @param failureHandler
     */
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

        // 成功处理
        DefaultCallHandler success = new DefaultCallHandler(false) {
            @Override
            public void handleCall(CallRecord callRecord) {
                if (successHandler.isInMainThread()) {
                    executeOnMainThread(() -> {
                        successHandler.handleCall(activeCall);
                    });
                }
                else {
                    execute(() -> {
                        successHandler.handleCall(activeCall);
                    });
                }

                if (callRecord.field.isPrivate()) {
                    // 私有场域，触发 Ringing 事件
                    ObservableEvent event = new ObservableEvent(MultipointCommEvent.Ringing, callRecord);
                    notifyObservers(event);

                    if (null != localVideoContainer) {
                        executeOnMainThread(() -> {
                            // 添加本地视频界面
                            localVideoContainer.addView(callRecord.field.getLocalDevice().getLocalVideoView());
                        });
                    }
                    if (null != remoteVideoContainer) {
                        executeOnMainThread(() -> {
                            // 添加对端视频界面
                            remoteVideoContainer.addView(callRecord.field.getLocalDevice().getRemoteVideoView());
                        });
                    }
                }
                else {
                    // 普通场域，触发 Ringing 事件
                    // TODO
                }
            }
        };

        // 失败处理
        StableFailureHandler failure = new StableFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                if (null != callTimer) {
                    callTimer.cancel();
                    callTimer = null;
                }

                error.data = activeCall;
                execute(failureHandler, error);

                ObservableEvent event = new ObservableEvent(MultipointCommEvent.Failed, error);
                notifyObservers(event);

                activeCall = null;
            }
        };

        // 创建通话记录
        this.activeCall = new CallRecord(this.privateField.getSelf(), this.privateField);
        // 设置主叫和被叫
        this.privateField.setCallRole(this.privateField.getFounder(), contact);
        // 设置媒体约束
        this.privateField.setMediaConstraint(mediaConstraint);

        execute(() -> {
            this.callTimer = new Timer();
            this.callTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    fireCallTimeout();
                }
            }, this.callTimeout);

            // 回调 InProgress 事件
            ObservableEvent event = new ObservableEvent(MultipointCommEvent.InProgress, activeCall);
            notifyObservers(event);
        });

        // 1. 申请一对一主叫
        this.privateField.applyCall(this.privateField.getCaller(), this.privateField.getSelf().device,
            new DefaultApplyCallHandler(false) {
                @Override
                public void handleApplyCall(CommField commField, Contact participant, Device device) {
                    // 记录媒体约束
                    activeCall.setCallerConstraint(mediaConstraint);

                    fillCommField(commField, true);

                    // 2. 启动 RTC 节点，发起 Offer
                    // 创建 RTC 设备
                    RTCDevice rtcDevice = createRTCDevice(RTCDevice.MODE_BIDIRECTION);
                    // 发起 Offer
                    privateField.launchOffer(rtcDevice, mediaConstraint, new DefaultCommFieldHandler() {
                        @Override
                        public void handleCommField(CommField commField) {
                            success.handleCall(activeCall);
                        }
                    }, new StableFailureHandler() {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            if (LogUtils.isDebugLevel()) {
                                LogUtils.d(TAG, "#launchOffer - Failed : " + error.code);
                            }

                            failure.handleFailure(MultipointComm.this, error);
                        }
                    });
                }
            }, new StableFailureHandler() {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    if (LogUtils.isDebugLevel()) {
                        LogUtils.d(TAG, "#applyCall - Failed : " + error.code);
                    }

                    failure.handleFailure(MultipointComm.this, error);
                }
            });
    }

    /**
     * 应答通话请求。
     *
     * @param mediaConstraint
     * @param successHandler
     * @param failureHandler
     */
    public void answerCall(MediaConstraint mediaConstraint, CallHandler successHandler, FailureHandler failureHandler) {
        if (null == this.activeCall) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.Failure.code);
            execute(failureHandler, error);
            return;
        }

        if (this.activeCall.field.isPrivate()) {
            Contact target = this.activeCall.field.getCaller();
            this.answerCall(mediaConstraint, target, successHandler, failureHandler);
        }
        else {
            // TODO
        }
    }

    public void answerCall(MediaConstraint mediaConstraint, Contact target, CallHandler successHandler, FailureHandler failureHandler) {
        if (null == this.offerSignaling) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.SignalingError.code);
            execute(failureHandler, error);
            return;
        }

        if (null != this.callTimer) {
            this.callTimer.cancel();
            this.callTimer = null;
        }

        execute(() -> {
            notifyObservers(new ObservableEvent(MultipointCommEvent.InProgress, this.activeCall));
        });

        if (this.activeCall.isActive()) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.CalleeBusy.code);
            execute(failureHandler, error);
            return;
        }

        // 记录时间
        this.activeCall.setAnswerTime(System.currentTimeMillis());

        // 1. 申请加入
        this.privateField.applyJoin(this.privateField.getSelf(), this.privateField.getSelf().device, new DefaultApplyCallHandler(false) {
            @Override
            public void handleApplyCall(CommField commField, Contact participant, Device device) {
                // 记录
                activeCall.setCalleeConstraint(mediaConstraint);

                fillCommField(commField, true);

                // 2. 启动 RTC 节点，发起 Answer
                RTCDevice rtcDevice = createRTCDevice(RTCDevice.MODE_BIDIRECTION);
                privateField.launchAnswer(rtcDevice, offerSignaling.sessionDescription, mediaConstraint, new DefaultCommFieldHandler() {
                    @Override
                    public void handleCommField(CommField commField) {
                        if (successHandler.isInMainThread()) {
                            executeOnMainThread(() -> {
                                successHandler.handleCall(activeCall);
                            });
                        }
                        else {
                            execute(() -> {
                                successHandler.handleCall(activeCall);
                            });
                        }

                        execute(() -> {
                            notifyObservers(new ObservableEvent(MultipointCommEvent.Connected, activeCall));
                        });
                    }
                }, new StableFailureHandler() {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {
                        if (failureHandler.isInMainThread()) {
                            executeOnMainThread(() -> {
                                failureHandler.handleFailure(MultipointComm.this, error);
                            });
                        }
                        else {
                            execute(() -> {
                                failureHandler.handleFailure(MultipointComm.this, error);
                            });
                        }

                        executeOnMainThread(() -> {
                            hangupCall();
                        });
                    }
                });
            }
        }, new StableFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {

            }
        });
    }

    /**
     * 终止当前的通话。
     */
    public void hangupCall() {
        this.hangupCall(new DefaultCommFieldHandler(false) {
            @Override
            public void handleCommField(CommField commField) {
                // Nothing
            }
        }, new StableFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                // Nothing
            }
        });
    }

    /**
     * 终止当前的通话。
     * @param successHandler 操作成功回调。
     * @param failureHandler 操作失败回调。
     */
    public void hangupCall(CommFieldHandler successHandler, FailureHandler failureHandler) {
        if (null == this.activeCall) {
            LogUtils.w(TAG, "#hangupCall - No active calling");
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.Failure.code);
            this.execute(failureHandler, error);
            return;
        }

        CommField field = this.activeCall.field;

        executeOnMainThread(() -> {
            if (field.isPrivate()) {
                if (null != localVideoContainer) {
                    localVideoContainer.removeAllViews();
                }
                if (null != remoteVideoContainer) {
                    remoteVideoContainer.removeAllViews();
                }
            }
            else {
                // TODO
            }
        });

        PipelineHandler handler = new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.ServerFault.code);
                    error.data = activeCall;

                    activeCall.setEndTime(System.currentTimeMillis());
                    activeCall.lastError = error;

                    executeOnMainThread(() -> {
                        field.close();
                    });

                    execute(failureHandler, error);
                    notifyObservers(new ObservableEvent(MultipointCommEvent.Failed, error));
                    activeCall = null;
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != MultipointCommState.Ok.code) {
                    ModuleError error = new ModuleError(MultipointComm.NAME, stateCode);
                    error.data = activeCall;

                    activeCall.setEndTime(System.currentTimeMillis());
                    activeCall.lastError = error;

                    executeOnMainThread(() -> {
                        field.close();
                    });

                    execute(failureHandler, error);
                    notifyObservers(new ObservableEvent(MultipointCommEvent.Failed, error));
                    activeCall = null;
                    return;
                }

                // 信令
                try {
                    Signaling signaling = new Signaling(packet.extractServiceData());
                    if (field.isPrivate()) {

                        executeOnMainThread(() -> {
                            field.close();
                        });

                        offerSignaling = null;
                        answerSignaling = null;

                        activeCall.setEndTime(System.currentTimeMillis());

                        if (successHandler.isInMainThread()) {
                            executeOnMainThread(() -> {
                                successHandler.handleCommField(field);
                            });
                        }
                        else {
                            execute(() -> {
                                successHandler.handleCommField(field);
                            });
                        }

                        notifyObservers(new ObservableEvent(MultipointCommEvent.Bye, activeCall));
                    }
                    else {
                        // 更新数据
                        field.update(signaling.field);

                        if (successHandler.isInMainThread()) {
                            executeOnMainThread(() -> {
                                successHandler.handleCommField(field);
                            });
                        }
                        else {
                            execute(() -> {
                                successHandler.handleCommField(field);
                            });
                        }

                        notifyObservers(new ObservableEvent(MultipointCommEvent.Bye, activeCall));

                        // 关闭场域
                        executeOnMainThread(() -> {
                            field.close();
                        });
                    }

                    // 重置
                    activeCall = null;
                } catch (JSONException e) {
                    LogUtils.w(TAG, e);
                }
            }
        };

        if (null != this.callTimer) {
            this.callTimer.cancel();
            this.callTimer = null;
        }

        if (field.isPrivate()) {
            if (this.activeCall.isActive()) {
                Signaling signaling = new Signaling(MultipointCommAction.Bye, field,
                        this.privateField.getFounder(), this.privateField.getSelf().device);
                Packet packet = new Packet(MultipointCommAction.Bye, signaling.toJSON());
                this.pipeline.send(MultipointComm.NAME, packet, handler);
            }
            else {
                // 回送被叫忙
                Signaling signaling = new Signaling(MultipointCommAction.Busy, field,
                        this.privateField.getFounder(), this.privateField.getSelf().device);
                Packet packet = new Packet(MultipointCommAction.Busy, signaling.toJSON());
                this.pipeline.send(MultipointComm.NAME, packet, handler);
            }
        }
        else {
            Signaling signaling = new Signaling(MultipointCommAction.Bye, field,
                    this.privateField.getSelf(), this.privateField.getSelf().device);
            Packet packet = new Packet(MultipointCommAction.Bye, signaling.toJSON());
            this.pipeline.send(MultipointComm.NAME, packet, handler);
        }
    }

    private void fireCallTimeout() {
        if (null != this.callTimer) {
            this.callTimer.cancel();
            this.callTimer = null;
        }

        CallRecord callRecord = this.activeCall;
        ObservableEvent event = new ObservableEvent(MultipointCommEvent.Timeout, callRecord);
        notifyObservers(event);

        hangupCall();
    }

    private RTCDevice createRTCDevice(String mode) {
        RTCDevice rtcDevice = new RTCDevice(this.getContext(), mode, this.peerConnectionFactory, this.eglBase.getEglBaseContext());
        rtcDevice.enableICE(this.iceServers);
        return rtcDevice;
    }

    private PeerConnectionFactory createPeerConnectionFactory() {
        PeerConnectionFactory.InitializationOptions initOptions = PeerConnectionFactory.InitializationOptions.builder(this.getContext()).createInitializationOptions();
        PeerConnectionFactory.initialize(initOptions);

        // 编码器
        VideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(this.eglBase.getEglBaseContext(), true, true);
        // 解码器
        VideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(this.eglBase.getEglBaseContext());

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.disableNetworkMonitor = true;

        // Audio Device module
//        JavaAudioDeviceModule audioDeviceModule = JavaAudioDeviceModule.builder(this.getContext())
//                .setSamplesReadyCallback(null)
//                .setUseHardwareAcousticEchoCanceler(false)
//                .setUseHardwareNoiseSuppressor(false)
//                .setAudioRecordErrorCallback(null)
//                .setAudioTrackErrorCallback(null)
//                .createAudioDeviceModule();

        PeerConnectionFactory factory = PeerConnectionFactory.builder()
                .setOptions(options)
//                .setAudioDeviceModule(audioDeviceModule)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();

        return factory;
    }

    @Override
    public void execute(Runnable task) {
        super.execute(task);
    }

    @Override
    public void executeOnMainThread(Runnable task) {
        super.executeOnMainThread(task);
    }

    @Override
    public void notifyObservers(ObservableEvent event) {
        super.notifyObservers(event);

        String eventName = event.getName();
        if (MultipointCommEvent.NewCall.equals(eventName)) {
            executeOnMainThread(() -> {
                for (CallListener listener : callListeners) {
                    listener.onNewCall((CallRecord) event.getData());
                }
            });
        }
        else if (MultipointCommEvent.InProgress.equals(eventName)) {
            executeOnMainThread(() -> {
                for (CallListener listener : callListeners) {
                    listener.onInProgress((CallRecord) event.getData());
                }
            });
        }
        else if (MultipointCommEvent.Ringing.equals(eventName)) {
            executeOnMainThread(() -> {
                for (CallListener listener : callListeners) {
                    listener.onRinging((CallRecord) event.getData());
                }
            });
        }
        else if (MultipointCommEvent.Connected.equals(eventName)) {
            executeOnMainThread(() -> {
                for (CallListener listener : callListeners) {
                    listener.onConnected((CallRecord) event.getData());
                }
            });
        }
        else if (MultipointCommEvent.Busy.equals(eventName)) {
            executeOnMainThread(() -> {
                for (CallListener listener : callListeners) {
                    listener.onBusy((CallRecord) event.getData());
                }
            });
        }
        else if (MultipointCommEvent.Bye.equals(eventName)) {
            executeOnMainThread(() -> {
                for (CallListener listener : callListeners) {
                    listener.onBye((CallRecord) event.getData());
                }
            });
        }
        else if (MultipointCommEvent.Timeout.equals(eventName)) {
            executeOnMainThread(() -> {
                for (CallListener listener : callListeners) {
                    listener.onTimeout((CallRecord) event.getData());
                }
            });
        }
        else if (MultipointCommEvent.Failed.equals(eventName)) {
            executeOnMainThread(() -> {
                for (CallListener listener : callListeners) {
                    listener.onFailed((ModuleError) event.getData());
                }
            });
        }
    }

    @Override
    public void update(ObservableEvent event) {
        if (ContactServiceEvent.SelfReady.equals(event.getName())) {
            Self self = (Self) event.getData();
            if (null == this.privateField) {
                this.privateField = new CommField(this, this.getContext(), self, this.pipeline);
            }
        }
    }

    protected void triggerOffer(Packet packet) {
        Signaling offerSignaling = null;
        try {
            offerSignaling = new Signaling(packet.extractServiceData());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (null != this.activeCall && this.activeCall.isActive()) {
            // 应答忙音 Busy
            Signaling busy = new Signaling(MultipointCommAction.Busy, offerSignaling.field,
                    this.privateField.getSelf(), this.privateField.getSelf().device);
            Packet busyPacket = new Packet(MultipointCommAction.Busy, busy.toJSON());
            this.pipeline.send(MultipointComm.NAME, busyPacket);
            return;
        }

        // 赋值
        this.offerSignaling = offerSignaling;

        // 启动应答定时器
        this.callTimer = new Timer();
        this.callTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                fireCallTimeout();
            }
        }, this.callTimeout - 10000);

        if (this.offerSignaling.field.isPrivate()) {
            this.activeCall = new CallRecord(this.privateField.getSelf(), this.privateField);

            this.privateField.setCaller(this.contactService.getContact(offerSignaling.caller.id));
            this.privateField.setCallee(this.contactService.getContact(offerSignaling.callee.id));

            this.activeCall.setCallerConstraint(offerSignaling.mediaConstraint);

            ObservableEvent event = new ObservableEvent(MultipointCommEvent.NewCall, this.activeCall);
            notifyObservers(event);
        }
        else {
            this.activeCall = new CallRecord(this.privateField.getSelf(), offerSignaling.field);

            // 填充数据
            fillCommField(this.activeCall.field, false);

            ObservableEvent event = new ObservableEvent(MultipointCommEvent.NewCall, this.activeCall);
            notifyObservers(event);
        }
    }

    protected void triggerAnswer(Packet packet) {
        if (null == this.activeCall || !this.activeCall.isActive()) {
            LogUtils.e(TAG, "#triggerAnswer no active call record");
            return;
        }

        if (null != this.callTimer) {
            this.callTimer.cancel();
            this.callTimer = null;
        }

        // 记录应答时间
        this.activeCall.setAnswerTime(System.currentTimeMillis());

        RTCDevice rtcDevice = null;

        JSONObject data = packet.extractServiceData();
        try {
            Signaling answerSignaling = new Signaling(data);
            if (this.activeCall.field.isPrivate()) {
                this.answerSignaling = answerSignaling;
                // 被叫媒体约束
                this.activeCall.setCalleeConstraint(answerSignaling.mediaConstraint);
                rtcDevice = this.activeCall.field.getRTCDevice();
            }
            else {
                if (answerSignaling.sn == 0) {
                    Contact fromContact = answerSignaling.contact;
                    CommFieldEndpoint endpoint = this.activeCall.field.getEndpoint(fromContact);
                    // 查找到对应的 RTC 设备
                    rtcDevice = this.activeCall.field.getRTCDevice(endpoint);
                }
                else {
                    // 通过 SN 查找 RTC 设备
                    rtcDevice = this.activeCall.field.getRTCDevice(answerSignaling.sn);
                }
            }
        } catch (JSONException e) {
            LogUtils.w(TAG, "#triggerAnswer", e);
        }

        if (null == rtcDevice) {
            LogUtils.w(TAG, "#triggerAnswer - Can NOT find rtc device for " + answerSignaling.contact.id);
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.NoPeerEndpoint.code);
            error.data = this.activeCall;
            execute(() -> {
                notifyObservers(new ObservableEvent(MultipointCommEvent.Failed, error));
            });
            return;
        }

        if (LogUtils.isDebugLevel()) {
            LogUtils.d(TAG, "Answer from " + answerSignaling.contact.id);
        }

        final RTCDevice currentDevice = rtcDevice;
        executeOnMainThread(() -> {
            currentDevice.doAnswer(answerSignaling.sessionDescription, new RTCDeviceHandler() {
                @Override
                public void handleRTCDevice(RTCDevice device) {
                    // 对于 recvonly 的 Peer 不回调 Connected 事件
                    if (device.getMode().equals(RTCDevice.MODE_BIDIRECTION) || device.getMode().equals(RTCDevice.MODE_SEND_ONLY)) {
                        notifyObservers(new ObservableEvent(MultipointCommEvent.Connected, activeCall));
                    }
                }
            }, new StableFailureHandler() {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    activeCall.lastError = error;
                    notifyObservers(new ObservableEvent(MultipointCommEvent.Failed, error));
                }
            });
        });
    }

    protected void triggerCandidate(Packet packet) {
        Signaling signaling = null;
        try {
            signaling = new Signaling(packet.extractServiceData());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RTCDevice rtcDevice = null;
        if (this.activeCall.field.isPrivate()) {
            rtcDevice = this.activeCall.field.getRTCDevice();
        }
        else {
            if (signaling.sn == 0) {
                // 找到对应的节点
                CommFieldEndpoint endpoint = this.activeCall.field.getEndpoint(signaling.contact);
                if (null != endpoint) {
                    rtcDevice = this.activeCall.field.getRTCDevice(endpoint);
                }
            }
            else {
                rtcDevice = this.activeCall.field.getRTCDevice(signaling.sn);
            }
        }

        if (null == rtcDevice) {
            LogUtils.e(MultipointComm.NAME, "#triggerCandidate - Can NOT find RTC device: " + signaling.name);
            return;
        }

        final RTCDevice currentDevice = rtcDevice;
        final Signaling currentSignaling = signaling;

        if (null != signaling.candidates) {
            executeOnMainThread(() -> {
                for (IceCandidate candidate : currentSignaling.candidates) {
                    currentDevice.doCandidate(candidate);
                }
            });
        }

        if (null != signaling.candidate) {
            executeOnMainThread(() -> {
                currentDevice.doCandidate(currentSignaling.candidate);
            });
        }
    }

    protected void triggerBye(Packet packet) {
        try {
            Signaling signaling = new Signaling(packet.extractServiceData());
            if (this.activeCall.field.isPrivate()) {
                hangupCall();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void triggerBusy(Packet packet) {
        if (null != this.callTimer) {
            this.callTimer.cancel();
            this.callTimer = null;
        }

        try {
            Signaling signaling = new Signaling(packet.extractServiceData());
            if (signaling.field.isPrivate()) {
                if (signaling.callee.id.longValue() == this.privateField.id.longValue()) {
                    // 被叫收到自己其他终端的 Busy

                    // 记录
                    this.activeCall.setEndTime(System.currentTimeMillis());

                    // 收到本终端的 Busy 时，回调 Bye
                    ObservableEvent event = new ObservableEvent(MultipointCommEvent.Bye, this.activeCall);
                    this.notifyObservers(event);
                }
                else {
                    // 收到其他终端的 Busy
                    ObservableEvent event = new ObservableEvent(MultipointCommEvent.Busy, this.activeCall);
                    notifyObservers(event);

                    // 终止通话
                    this.hangupCall();
                }
            }
            else {
                ObservableEvent event = new ObservableEvent(MultipointCommEvent.Busy, this.activeCall);
                notifyObservers(event);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void fillCommField(CommField commField, boolean force) {
        if (null == commField.getSelf() || force) {
            // 数据对象赋值
            if (null == commField.getSelf()) {
                commField.assigns(this, getContext(), this.privateField.getSelf(), this.pipeline);
            }

            for (CommFieldEndpoint endpoint : commField.getEndpoints()) {
                Contact contact = endpoint.getContact();
                if (null != contact) {
                    contact = this.contactService.getContact(contact.id);
                    if (null != contact) {
                        endpoint.setContact(contact);
                    }
                }
            }

            Contact founder = commField.getFounder();
            founder = this.contactService.getContact(founder.id);
            if (null != founder) {
                commField.setFounder(founder);
            }

            Contact caller = commField.getCaller();
            if (null != caller) {
                caller = this.contactService.getContact(caller.id);
                if (null != caller) {
                    commField.setCaller(caller);
                }
            }

            Contact callee = commField.getCallee();
            if (null != callee) {
                callee = this.contactService.getContact(callee.id);
                if (null != callee) {
                    commField.setCallee(callee);
                }
            }

            Group group = commField.getGroup();
            if (null != group) {
                group = this.contactService.getGroup(group.id);
                if (null != group) {
                    commField.setGroup(group);
                }
            }
        }
    }
}
