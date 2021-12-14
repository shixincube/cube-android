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

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import cube.contact.ContactService;
import cube.contact.ContactServiceEvent;
import cube.contact.model.Contact;
import cube.contact.model.Device;
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
import cube.multipointcomm.model.CallRecord;
import cube.multipointcomm.model.CommField;
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

    private EglBase eglBase;

    private PeerConnectionFactory peerConnectionFactory;

    private List<PeerConnection.IceServer> iceServers;

    private Timer callTimer;
    private long callTimeout = 30000;

    private CommField privateField;

    private ConcurrentHashMap<Long, CommField> commFieldMap;

    private CallRecord activeCall;

    private Signaling offerSignaling;
    private Signaling answerSignaling;

    public MultipointComm() {
        super(NAME);
    }

    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }

        this.commFieldMap = new ConcurrentHashMap<>();

        this.eglBase = EglBase.create();
        this.peerConnectionFactory = createPeerConnectionFactory();

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
                            failure.handleFailure(MultipointComm.this, error);
                        }
                    });
                }
            }, new StableFailureHandler() {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    failure.handleFailure(MultipointComm.this, error);
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

        PipelineHandler handler = new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.ServerFault.code);
                    error.data = activeCall;

                    activeCall.setEndTime(System.currentTimeMillis());
                    activeCall.lastError = error;

                    field.close();

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

                    field.close();

                    execute(failureHandler, error);
                    notifyObservers(new ObservableEvent(MultipointCommEvent.Failed, error));
                    activeCall = null;
                    return;
                }

                // 信令
                try {
                    Signaling signaling = new Signaling(packet.extractServiceData());
                    if (field.isPrivate()) {
                        field.close();
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
                        field.close();
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

        this.hangupCall();
    }

    private RTCDevice createRTCDevice(String mode) {
        return new RTCDevice(this.getContext(), mode, this.peerConnectionFactory, this.eglBase.getEglBaseContext());
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
        JavaAudioDeviceModule audioDeviceModule = JavaAudioDeviceModule.builder(this.getContext())
                .setSamplesReadyCallback(null)
                .setUseHardwareAcousticEchoCanceler(false)
                .setUseHardwareNoiseSuppressor(false)
                .setAudioRecordErrorCallback(null)
                .setAudioTrackErrorCallback(null)
                .createAudioDeviceModule();

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
    public void update(ObservableEvent event) {
        if (ContactServiceEvent.SelfReady.equals(event.getName())) {
            Self self = (Self) event.getData();
            if (null == this.privateField) {
                this.privateField = new CommField(this, this.getContext(), self, this.pipeline);
            }
        }
    }
}
