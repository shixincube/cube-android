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
import org.webrtc.SessionDescription;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import cell.util.Utils;
import cube.contact.ContactService;
import cube.contact.ContactServiceEvent;
import cube.contact.handler.StableGroupAppendixHandler;
import cube.contact.model.AbstractContact;
import cube.contact.model.Contact;
import cube.contact.model.Device;
import cube.contact.model.Group;
import cube.contact.model.GroupAppendix;
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
import cube.multipointcomm.handler.DefaultApplyHandler;
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
public class MultipointComm extends Module implements Observer, MediaListener {

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

    private List<MultipointCallListener> multipointCallListeners;

    private Signaling offerSignaling;
    private Signaling answerSignaling;

    private ViewGroup localVideoContainer;
    private ViewGroup remoteVideoContainer;

    private VideoContainerAgent videoContainerAgent;
    private List<ViewGroup> videoContainers;

    public MultipointComm() {
        super(NAME);
        this.callListeners = new ArrayList<>();
        this.multipointCallListeners = new ArrayList<>();
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
            this.privateField.setMediaListener(this);
        }

        return true;
    }

    @Override
    public void stop() {
        super.stop();

        this.videoContainerAgent = null;

        if (null != this.videoContainers) {
            executeOnMainThread(() -> {
                for (ViewGroup viewGroup : this.videoContainers) {
                    viewGroup.removeAllViews();
                }
                this.videoContainers.clear();
            });
        }

        this.callListeners.clear();

        if (null != this.pipelineListener) {
            this.pipeline.removeListener(MultipointComm.NAME, this.pipelineListener);
        }

        if (null != this.activeCall) {
            executeOnMainThread(() -> {
                hangupCall();
            });
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
     * 添加通话监听器。
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
     * 添加多方通话监听器。
     *
     * @param listener
     */
    public void addMultipointCallListener(MultipointCallListener listener) {
        if (!this.multipointCallListeners.contains(listener)) {
            this.multipointCallListeners.add(listener);
        }
    }

    /**
     * 移除多方通话监听器。
     *
     * @param listener
     */
    public void removeMultipointCallListener(MultipointCallListener listener) {
        this.multipointCallListeners.remove(listener);
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
     * 设置视频容器代理。
     *
     * @param videoContainerAgent
     */
    public void setVideoContainerAgent(VideoContainerAgent videoContainerAgent) {
        this.videoContainerAgent = videoContainerAgent;
        if (null == this.videoContainers) {
            this.videoContainers = new ArrayList<>();
        }
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
        this.makeCall((AbstractContact) contact, mediaConstraint, successHandler, failureHandler);
    }

    /**
     * 发起通话。
     *
     * @param group
     * @param mediaConstraint
     * @param successHandler
     * @param failureHandler
     */
    public void makeCall(Group group, MediaConstraint mediaConstraint, CallHandler successHandler, FailureHandler failureHandler) {
        this.makeCall((AbstractContact) group, mediaConstraint, successHandler, failureHandler);
    }

    /**
     * 发起通话。
     *
     * @param target
     * @param mediaConstraint
     * @param successHandler
     * @param failureHandler
     */
    private void makeCall(AbstractContact target, MediaConstraint mediaConstraint, CallHandler successHandler, FailureHandler failureHandler) {
        if (null == this.privateField) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.NoCommField.code);
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

                    // 绑定视频 View
                    bindVideoView(callRecord.field);
                }
                else {
                    // 普通场域，触发 Ringing 事件
                    ObservableEvent event = new ObservableEvent(MultipointCommEvent.Ringing, callRecord);
                    notifyObservers(event);

                    // 请求接收其他终端的数据
                    if (callRecord.field.getMediaConstraint().videoEnabled) {
                        // 多方视频采用 SFU 模式
                        Long selfId = privateField.getSelf().id;
                        executeOnMainThread(() -> {
                            for (CommFieldEndpoint endpoint : callRecord.field.getEndpoints()) {
                                if (endpoint.getContact().id.longValue() == selfId.longValue()) {
                                    continue;
                                }

                                // 接收指定终端的数据
                                follow(endpoint, null, null);
                            }
                        });
                    }
                    else {
                        // 多方语音采用 MCU 模式
                        executeOnMainThread(() -> {
                            touch(callRecord.field, null, null);
                        });
                    }
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

                if (null != activeCall) {
                    executeOnMainThread(() -> {
                        hangupCall();
                    });
                }
            }
        };

        if (target instanceof Contact) {
            // 呼叫指定联系人

            if (null != this.activeCall && this.activeCall.isActive()) {
                ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.CallerBusy.code);
                execute(failureHandler, error);
                return;
            }

            Contact contact = (Contact) target;

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
                    new DefaultApplyHandler(false) {
                        @Override
                        public void handleApply(CommField commField, Contact participant, Device device) {
                            // 记录媒体约束
                            activeCall.setCallerConstraint(mediaConstraint);

                            fillCommField(commField, true);

                            // 2. 启动 RTC 节点，发起 Offer
                            // 创建 RTC 设备
                            RTCDevice rtcDevice = createRTCDevice(RTCDevice.MODE_BIDIRECTION, null, null);
                            // 发起 Offer
                            privateField.launchOffer(rtcDevice, mediaConstraint, new DefaultCommFieldHandler() {
                                @Override
                                public void handleCommField(CommField commField) {
                                    // 填充数据
                                    fillCommField(commField, true);

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
        else if (target instanceof Group) {
            // 发起群组内的通话
            // 获取群组的通讯 ID
            Group group = (Group) target;

            execute(() -> {
                Long commId = group.getAppendix().getCommId();
                if (commId.longValue() == 0) {
                    // 创建新场域，关联对应的群组
                    if (LogUtils.isDebugLevel()) {
                        LogUtils.d(TAG, "#makeCall - Create comm field for group " + group.id);
                    }

                    createCommField(mediaConstraint, group, new DefaultCommFieldHandler(false) {
                        @Override
                        public void handleCommField(CommField commField) {
                            // 更新群组的 CommFiled ID
                            group.getAppendix().updateCommId(commField.id, new StableGroupAppendixHandler() {
                                @Override
                                public void handleAppendix(Group group, GroupAppendix appendix) {
                                    // 发起呼叫
                                    executeOnMainThread(() -> {
                                        makeCall(commField, mediaConstraint, successHandler, failureHandler);
                                    });
                                }
                            }, new StableFailureHandler() {
                                @Override
                                public void handleFailure(Module module, ModuleError error) {
                                    if (LogUtils.isDebugLevel()) {
                                        LogUtils.d(TAG, "#makeCall - #createCommField - #updateCommId - error : " + error.code);
                                    }

                                    failure.handleFailure(module, error);
                                }
                            });
                        }
                    }, new StableFailureHandler() {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            if (LogUtils.isDebugLevel()) {
                                LogUtils.d(TAG, "#makeCall - #createCommField - error : " + error.code);
                            }

                            failure.handleFailure(module, error);
                        }
                    });
                }
                else {
                    // 获取场域
                    if (LogUtils.isDebugLevel()) {
                        LogUtils.d(TAG, "#makeCall - Get comm field: " + commId);
                    }

                    getCommField(commId, new DefaultCommFieldHandler(false) {
                        @Override
                        public void handleCommField(CommField commField) {
                            // 发起呼叫
                            executeOnMainThread(() -> {
                                makeCall(commField, mediaConstraint, successHandler, failureHandler);
                            });
                        }
                    }, new StableFailureHandler() {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            failure.handleFailure(module, error);
                        }
                    });
                }
            });
        }
        else if (target instanceof CommField) {
            CommField field = (CommField) target;

            if (null != this.activeCall && null != this.activeCall.getCommField()) {
                if (this.activeCall.getCommField().id.longValue() != field.id.longValue()) {
                    LogUtils.w(TAG, "Comm field data error");
                    ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.CommFieldStateError.code);
                    error.data = this.activeCall;
                    execute(failureHandler, error);
                    return;
                }
            }

            if (null == this.activeCall) {
                this.activeCall = new CallRecord(this.privateField.getSelf(), field);
            }

            execute(() -> {
                notifyObservers(new ObservableEvent(MultipointCommEvent.InProgress, activeCall));
            });

            if (LogUtils.isDebugLevel()) {
                LogUtils.d(TAG, "#makeCall - Apply call for comm field: " + field.getName());
            }

            // 1. 申请通话
            field.applyCall(this.privateField.getSelf(), this.privateField.getSelf().device, new DefaultApplyHandler(false) {
                @Override
                public void handleApply(CommField commField, Contact participant, Device device) {
                    // 获取自己的终端节点
                    CommFieldEndpoint endpoint = commField.getEndpoint(privateField.getSelf());
                    if (null == endpoint) {
                        LogUtils.e(TAG, "#makeCall - Can NOT find endpoint in \"" + commField.getName() + "\"");
                    }

                    field.update(commField);

                    fillCommField(field, true);

                    ViewGroup videoContainer = null;
                    if (mediaConstraint.videoEnabled) {
                        if (null != videoContainerAgent) {
                            videoContainer = videoContainerAgent.getVideoContainer(endpoint);
                        }
                        else {
                            videoContainer = localVideoContainer;
                        }
                    }

                    if (LogUtils.isDebugLevel()) {
                        LogUtils.d(TAG, "#makeCall - Open offer for comm field: " + field.getName());
                    }

                    // 2. 发起 Offer
                    RTCDevice rtcDevice = createRTCDevice(RTCDevice.MODE_SEND_ONLY, videoContainer, null);
                    field.launchOffer(rtcDevice, mediaConstraint, endpoint, new DefaultCommFieldHandler(false) {
                        @Override
                        public void handleCommField(CommField commField) {
                            success.handleCall(activeCall);
                        }
                    }, failure);
                }
            }, new StableFailureHandler() {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    failure.handleFailure(MultipointComm.this, error);
                }
            });
        }
        else {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.Failure.code);
            error.data = target;
            execute(failureHandler, error);
        }
    }

    /**
     * 邀请列表里的联系人加入到当前通讯域。
     *
     * @param commField
     * @param contacts
     * @param successHandler
     * @param failureHandler
     */
    public void inviteCall(CommField commField, List<Contact> contacts, CommFieldHandler successHandler, FailureHandler failureHandler) {
        this.inviteCall(commField.id, contacts, successHandler, failureHandler);
    }

    /**
     * 邀请列表里的联系人加入到当前通讯域。
     *
     * @param group
     * @param contacts
     * @param successHandler
     * @param failureHandler
     */
    public void inviteCall(Group group, List<Contact> contacts, CommFieldHandler successHandler, FailureHandler failureHandler) {
        execute(() -> {
            final Long commId = group.getAppendix().getCommId();
            executeOnMainThread(() -> {
                inviteCall(commId, contacts, successHandler, failureHandler);
            });
        });
    }

    /**
     * 邀请列表里的联系人加入到当前 ID 的通讯域。
     *
     * @param commId
     * @param contacts
     * @param successHandler
     * @param failureHandler
     */
    private void inviteCall(Long commId, List<Contact> contacts, CommFieldHandler successHandler, FailureHandler failureHandler) {
        DefaultCommFieldHandler handler = new DefaultCommFieldHandler() {
            @Override
            public void handleCommField(CommField commField) {
                Signaling signaling = new Signaling(MultipointCommAction.Invite, commField,
                        privateField.getSelf(), privateField.getSelf().device);

                // 设置邀请列表
                List<Long> invitees = new ArrayList<>();
                List<Contact> inviteeContacts = new ArrayList<>();
                for (Contact contact : contacts) {
                    if (privateField.getSelf().equals(contact)) {
                        // 跳过自己
                        continue;
                    }

                    inviteeContacts.add(contact);
                    invitees.add(contact.id);
                }
                signaling.invitees = invitees;

                Packet requestPacket = new Packet(MultipointCommAction.Invite, signaling.toJSON());
                pipeline.send(MultipointComm.NAME, requestPacket, new PipelineHandler() {
                    @Override
                    public void handleResponse(Packet packet) {
                        if (packet.state.code != PipelineState.Ok.code) {
                            ModuleError error = new ModuleError(MultipointComm.NAME, packet.state.code);
                            error.data = commField;
                            execute(failureHandler, error);
                            return;
                        }

                        int stateCode = packet.extractServiceStateCode();
                        if (stateCode != MultipointCommState.Ok.code) {
                            ModuleError error = new ModuleError(MultipointComm.NAME, stateCode);
                            error.data = commField;
                            execute(failureHandler, error);
                            return;
                        }

                        if (successHandler.isInMainThread()) {
                            executeOnMainThread(() -> {
                                successHandler.handleCommField(commField);
                            });
                        }
                        else {
                            execute(() -> {
                                successHandler.handleCommField(commField);
                            });
                        }

                        execute(() -> {
                            notifyObservers(new ObservableEvent(MultipointCommEvent.Invite, commField, inviteeContacts));
                        });
                    }
                });
            }
        };

        if (null != this.activeCall && this.activeCall.field.id.longValue() == commId.longValue()) {
            handler.handleCommField(this.activeCall.field);
        }
        else {
            this.getCommField(commId, new DefaultCommFieldHandler(false) {
                @Override
                public void handleCommField(CommField commField) {
                    handler.handleCommField(commField);
                }
            }, new StableFailureHandler() {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    if (failureHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            failureHandler.handleFailure(module, error);
                        });
                    }
                    else {
                        execute(() -> {
                            failureHandler.handleFailure(module, error);
                        });
                    }
                }
            });
        }
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

        if (null == this.offerSignaling) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.SignalingError.code);
            execute(failureHandler, error);
            return;
        }

        if (this.activeCall.field.isPrivate()) {
            if (null != this.callTimer) {
                this.callTimer.cancel();
                this.callTimer = null;
            }

            if (this.activeCall.isActive()) {
                ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.CalleeBusy.code);
                execute(failureHandler, error);
                return;
            }

            execute(() -> {
                notifyObservers(new ObservableEvent(MultipointCommEvent.InProgress, this.activeCall));
            });

            // 记录时间
            this.activeCall.setAnswerTime(System.currentTimeMillis());

            // 1. 申请加入
            this.privateField.applyJoin(this.privateField.getSelf(), this.privateField.getSelf().device, new DefaultApplyHandler(false) {
                @Override
                public void handleApply(CommField commField, Contact participant, Device device) {
                    // 记录
                    activeCall.setCalleeConstraint(mediaConstraint);

                    fillCommField(commField, true);

                    // 2. 启动 RTC 节点，发起 Answer
                    RTCDevice rtcDevice = createRTCDevice(RTCDevice.MODE_BIDIRECTION, null, null);
                    privateField.launchAnswer(rtcDevice, offerSignaling.sessionDescription, mediaConstraint, new DefaultCommFieldHandler() {
                        @Override
                        public void handleCommField(CommField commField) {
                            // 填充数据
                            fillCommField(commField, true);

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

                            // 绑定视频 View
                            bindVideoView(activeCall.field);

                            execute(() -> {
                                notifyObservers(new ObservableEvent(MultipointCommEvent.Connected, activeCall));
                            });
                        }
                    }, new StableFailureHandler() {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            if (LogUtils.isDebugLevel()) {
                                LogUtils.d(TAG, "#launchAnswer - Failed : " + error.code);
                            }

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
                    if (LogUtils.isDebugLevel()) {
                        LogUtils.d(TAG, "#applyJoin - Failed : " + error.code);
                    }

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
                }
            });
        }
        else {
            // TODO
        }
    }

    /**
     * 终止当前的通话。
     */
    public void hangupCall() {
        this.hangupCall(new DefaultCallHandler(false) {
            @Override
            public void handleCall(CallRecord callRecord) {
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
    public void hangupCall(CallHandler successHandler, FailureHandler failureHandler) {
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
                if (null != videoContainers) {
                    for (ViewGroup viewGroup : videoContainers) {
                        viewGroup.removeAllViews();
                    }
                    videoContainers.clear();
                }
            }
        });

        PipelineHandler handler = new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    if (null != activeCall) {
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
                    }
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

                final CallRecord record = activeCall;

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
                                successHandler.handleCall(record);
                                activeCall = null;
                            });
                        }
                        else {
                            execute(() -> {
                                successHandler.handleCall(record);
                                activeCall = null;
                            });
                        }

                        notifyObservers(new ObservableEvent(MultipointCommEvent.Bye, record));
                    }
                    else {
                        // 更新数据
                        field.update(signaling.field);

                        if (successHandler.isInMainThread()) {
                            executeOnMainThread(() -> {
                                successHandler.handleCall(record);
                                activeCall = null;
                            });
                        }
                        else {
                            execute(() -> {
                                successHandler.handleCall(record);
                                activeCall = null;
                            });
                        }

                        notifyObservers(new ObservableEvent(MultipointCommEvent.Bye, record));

                        // 关闭场域
                        executeOnMainThread(() -> {
                            field.close();
                        });
                    }
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
                signaling.caller = this.privateField.getCaller();
                signaling.callee = this.privateField.getCallee();
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

    /**
     * 从指定 Comm Field 上接收混码流。
     *
     * @param commField
     * @param successHandler
     * @param failureHandler
     */
    private void touch(CommField commField, @Nullable CallHandler successHandler, @Nullable FailureHandler failureHandler) {
        if (commField.getMediaConstraint().videoEnabled && null == this.remoteVideoContainer) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.NoVideoContainer.code);
            error.data = commField;
            if (null != failureHandler) {
                execute(failureHandler, error);
            }
            return;
        }

        if (null == this.activeCall) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.InvalidCallRecord.code);
            error.data = commField;
            if (null != failureHandler) {
                execute(failureHandler, error);
            }
            return;
        }

        if (LogUtils.isDebugLevel()) {
            LogUtils.d(TAG, "#touch - Touch comm filed: " + commField.getName());
        }

        executeOnMainThread(() -> {
            // 创建 RTC device
            RTCDevice rtcDevice = this.createRTCDevice(RTCDevice.MODE_RECEIVE_ONLY, null, remoteVideoContainer);
            // 发起 recv only 的 Offer
            commField.launchOffer(rtcDevice, commField.getMediaConstraint(), new DefaultCommFieldHandler() {
                @Override
                public void handleCommField(CommField commField) {
                    if (null != successHandler) {
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
                    }
                }
            }, new StableFailureHandler() {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    if (null != failureHandler) {
                        execute(failureHandler, error);
                    }
                }
            });
        });
    }

    /**
     * 定向接收指定终端的音视频数据。
     *
     * @param endpoint
     * @param successHandler
     * @param failureHandler
     */
    private void follow(CommFieldEndpoint endpoint, @Nullable CallHandler successHandler, @Nullable FailureHandler failureHandler) {
        if (null == this.privateField) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.Uninitialized.code);
            if (null != failureHandler) {
                execute(failureHandler, error);
            }
            return;
        }

        StableFailureHandler failure = new StableFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                if (null != activeCall) {
                    activeCall.lastError = error;
                }

                if (null != failureHandler) {
                    execute(failureHandler, error);
                }

                if (null != activeCall) {
                    executeOnMainThread(() -> {
                        activeCall.field.closeRTCDevice(endpoint);
                    });
                }
            }
        };

        if (null == this.activeCall) {
            if (null == endpoint.getField()) {
                ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.Uninitialized.code);
                failure.handleFailure(MultipointComm.this, error);
                LogUtils.e(TAG, error.toString());
                return;
            }

            // 创建记录
            this.activeCall = new CallRecord(this.contactService.getSelf(), endpoint.getField());
        }
        else {
            if (this.activeCall.field.isPrivate()) {
                ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.CommFieldStateError.code);
                failure.handleFailure(MultipointComm.this, error);
                LogUtils.e(TAG, error.toString());
                return;
            }
        }

        if (this.activeCall.field.hasRTCDevice(endpoint)) {
            LogUtils.w(TAG, "#follow - RTC device is working");
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.CommFieldStateError.code);
            if (null != failureHandler) {
                execute(failureHandler, error);
            }
            return;
        }

        if (null == this.videoContainerAgent || null == this.videoContainers) {
            LogUtils.w(TAG, "#follow - video container agent is null");
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.VideoContainerAgentNotSetting.code);
            failure.handleFailure(MultipointComm.this, error);
            return;
        }

        ViewGroup remoteVideoViewGroup = this.videoContainerAgent.getVideoContainer(endpoint);
        if (null == remoteVideoViewGroup) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.NoVideoContainer.code);
            failure.handleFailure(MultipointComm.this, error);
            return;
        }

        if (!this.videoContainers.contains(remoteVideoViewGroup)) {
            this.videoContainers.add(remoteVideoViewGroup);
        }

        // TODO 检查是否已经接收该终端数据

        if (LogUtils.isDebugLevel()) {
            LogUtils.d(TAG, "Follow endpoint " + endpoint.id);
        }

        executeOnMainThread(() -> {
            // 创建 RTC device
            RTCDevice rtcDevice = this.createRTCDevice(RTCDevice.MODE_RECEIVE_ONLY, null, remoteVideoViewGroup);

            // 发起 recv only 的 Offer
            this.activeCall.field.launchOffer(rtcDevice, this.activeCall.field.getMediaConstraint(),
                    endpoint, new DefaultCommFieldHandler(false) {
                        @Override
                        public void handleCommField(CommField commField) {
                            // 填充数据
                            fillCommField(commField, true);

                            if (null != successHandler) {
                                if (successHandler.isInMainThread()) {
                                    executeOnMainThread(() -> {
                                        successHandler.handleCall(activeCall);
                                    });
                                }
                                else {
                                    successHandler.handleCall(activeCall);
                                }
                            }

                            notifyObservers(new ObservableEvent(MultipointCommEvent.Followed, endpoint));
                        }
                    }, failure);
        });
    }

    /**
     * 取消定向接收的指定终端音视频数据。
     *
     * @param endpoint
     * @param successHandler
     * @param failureHandler
     */
    private void unfollow(CommFieldEndpoint endpoint, @Nullable CallHandler successHandler, @Nullable FailureHandler failureHandler) {
        if (null == this.privateField) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.Uninitialized.code);
            if (null != failureHandler) {
                execute(failureHandler, error);
            }
            return;
        }

        if (null == this.activeCall) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.InvalidCallRecord.code);
            if (null != failureHandler) {
                execute(failureHandler, error);
            }
            return;
        }

        Signaling signaling = new Signaling(MultipointCommAction.Bye, this.activeCall.field,
                this.privateField.getSelf(), this.privateField.getSelf().device);
        // 设置目录
        signaling.target = endpoint;

        Packet requestPacket = new Packet(MultipointCommAction.Bye, signaling.toJSON());
        this.pipeline.send(MultipointComm.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(MultipointComm.NAME, packet.state.code);
                    if (null != failureHandler) {
                        execute(failureHandler, error);
                    }
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != MultipointCommState.Ok.code) {
                    ModuleError error = new ModuleError(MultipointComm.NAME, stateCode);
                    if (null != failureHandler) {
                        execute(failureHandler, error);
                    }
                    return;
                }

                // 关闭 Endpoint
                activeCall.field.closeEndpoint(endpoint);

                // 关闭 RTC 设备
                activeCall.field.closeRTCDevice(endpoint);

                // 发送 Unfollowed 事件
                notifyObservers(new ObservableEvent(MultipointCommEvent.Unfollowed, endpoint));

                if (null != successHandler) {
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
                }
            }
        });
    }

    private void bindVideoView(CommField commField) {
        if (null != remoteVideoContainer) {
            executeOnMainThread(() -> {
                // 添加对端视频界面
                remoteVideoContainer.removeAllViews();
                remoteVideoContainer.addView(commField.getLocalDevice().getRemoteVideoView());
            });
        }

        if (null != localVideoContainer) {
            executeOnMainThread(() -> {
                // 添加本地视频界面
                localVideoContainer.removeAllViews();
                localVideoContainer.addView(commField.getLocalDevice().getLocalVideoView());
            });
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

    private RTCDevice createRTCDevice(String mode, ViewGroup localVideoContainer, ViewGroup remoteVideoContainer) {
        RTCDevice rtcDevice = new RTCDevice(this.getContext(), mode, this.peerConnectionFactory, this.eglBase.getEglBaseContext());
        rtcDevice.enableICE(this.iceServers);

        if (null != localVideoContainer) {
            localVideoContainer.addView(rtcDevice.getLocalVideoView());
        }

        if (null != remoteVideoContainer) {
            remoteVideoContainer.addView(rtcDevice.getRemoteVideoView());
        }

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

    /**
     * 创建场域。
     *
     * @param mediaConstraint
     * @param group
     * @param successHandler
     * @param failureHandler
     */
    private void createCommField(MediaConstraint mediaConstraint, Group group, CommFieldHandler successHandler,
                                      FailureHandler failureHandler) {
        CommField commField = new CommField(Utils.generateUnsignedSerialNumber(), this, this.getContext(),
                this.contactService.getSelf(), this.pipeline, group, mediaConstraint);
        commField.setMediaListener(this);

        // 向服务器申请创建场域
        Packet requestPacket = new Packet(MultipointCommAction.CreateField, commField.toJSON());
        this.pipeline.send(MultipointComm.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(MultipointComm.NAME, packet.state.code);
                    error.data = commField;
                    execute(failureHandler, error);
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != MultipointCommState.Ok.code) {
                    ModuleError error = new ModuleError(MultipointComm.NAME, stateCode);
                    error.data = commField;
                    execute(failureHandler, error);
                    return;
                }

                execute(() -> {
                    successHandler.handleCommField(commField);
                });
            }
        });
    }

    private void getCommField(Long commId, CommFieldHandler successHandler, FailureHandler failureHandler) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("commFieldId", commId.longValue());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Packet requestPacket = new Packet(MultipointCommAction.GetField, payload);
        this.pipeline.send(MultipointComm.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(MultipointComm.NAME, packet.state.code);
                    error.data = commId;
                    execute(failureHandler, error);
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != MultipointCommState.Ok.code) {
                    ModuleError error = new ModuleError(MultipointComm.NAME, stateCode);
                    error.data = commId;
                    execute(failureHandler, error);
                    return;
                }

                execute(() -> {
                    CommField commField = null;
                    try {
                        commField = new CommField(packet.extractServiceData());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // 填充数据
                    fillCommField(commField, true);

                    successHandler.handleCommField(commField);
                });
            }
        });
    }

    @Override
    public void onMediaConnected(CommField commField, RTCDevice device) {

    }

    @Override
    public void onMediaDisconnected(CommField commField, RTCDevice device) {

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
        else if (MultipointCommEvent.Invite.equals(eventName)) {
            executeOnMainThread(() -> {
                for (MultipointCallListener listener : multipointCallListeners) {
                    listener.onInvite((CommField) event.getData(), (List<Contact>) event.getSecondaryData());
                }
            });
        }
        else if (MultipointCommEvent.Invited.equals(eventName)) {
            executeOnMainThread(() -> {
                for (MultipointCallListener listener : multipointCallListeners) {
                    listener.onInvited((CommField) event.getData(), (Contact) event.getSecondaryData());
                }
            });
        }
        else if (MultipointCommEvent.Arrived.equals(eventName)) {
            CommFieldEndpoint endpoint = (CommFieldEndpoint) event.getData();
            executeOnMainThread(() -> {
                for (MultipointCallListener listener : multipointCallListeners) {
                    listener.onEndpointArrived(endpoint.getField(), endpoint);
                }
            });
        }
        else if (MultipointCommEvent.Left.equals(eventName)) {
            CommFieldEndpoint endpoint = (CommFieldEndpoint) event.getData();
            executeOnMainThread(() -> {
                for (MultipointCallListener listener : multipointCallListeners) {
                    listener.onEndpointLeft(endpoint.getField(), endpoint);
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
                this.privateField.setMediaListener(this);
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
            // 创建 CallRecord
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
        Signaling answerSignaling = null;
        try {
            answerSignaling = new Signaling(data);
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
            LogUtils.d(TAG, "Answer from " + answerSignaling.contact.id + " [" + rtcDevice.getMode() + "]");
        }

        final RTCDevice currentDevice = rtcDevice;
        final SessionDescription sessionDescription = answerSignaling.sessionDescription;
        executeOnMainThread(() -> {
            currentDevice.doAnswer(sessionDescription, new RTCDeviceHandler() {
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

    protected void triggerInvite(Packet packet) {
        if (null != this.activeCall && this.activeCall.isActive()) {
            // 正在通话
            return;
        }

        try {
            Signaling signaling = new Signaling(packet.extractServiceData());

            if (LogUtils.isDebugLevel()) {
                LogUtils.d(TAG, "Receive \"" + signaling.field.getName() + "\" invitation");
            }

            fillCommField(signaling.field, true);

            Contact inviter = this.contactService.getContact(signaling.contact.id);

            this.activeCall = new CallRecord(this.privateField.getSelf(), signaling.field);

            notifyObservers(new ObservableEvent(MultipointCommEvent.Invited,
                    this.activeCall.field, inviter));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void triggerArrived(Packet packet) {
        JSONObject data = packet.extractServiceData();
        CommField commField = null;
        CommFieldEndpoint endpoint = null;

        try {
            commField = new CommField(data.getJSONObject("field"));
            endpoint = new CommFieldEndpoint(data.getJSONObject("endpoint"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (commField.id.longValue() == this.activeCall.field.id.longValue()) {
            this.activeCall.field.update(commField);
            fillCommField(this.activeCall.field, true);
        }

        endpoint.setField(this.activeCall.field);

        if (LogUtils.isDebugLevel()) {
            LogUtils.d(TAG, "Endpoint \"" + endpoint.getName() + "\" arrived \"" + commField.getName() + "\"");
        }

        // 事件
        notifyObservers(new ObservableEvent(MultipointCommEvent.Arrived, endpoint));

        if (commField.id.longValue() == this.activeCall.field.id.longValue() && commField.getMediaConstraint().videoEnabled) {
            // 使用了视频，采用 SFU 方式
            CommFieldEndpoint target = endpoint;
            executeOnMainThread(() -> {
                follow(target, null, null);
            });
        }
    }

    protected void triggerLeft(Packet packet) {
        JSONObject data = packet.extractServiceData();
        CommField commField = null;
        CommFieldEndpoint endpoint = null;

        try {
            commField = new CommField(data.getJSONObject("field"));
            endpoint = new CommFieldEndpoint(data.getJSONObject("endpoint"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        endpoint.setField(commField);

        if (LogUtils.isDebugLevel()) {
            LogUtils.d(TAG, "Endpoint \"" + endpoint.getName() + "\" left \"" + commField.getName() + "\"");
        }

        if (null != this.activeCall && commField.id.longValue() == this.activeCall.field.id.longValue()) {
            RTCDevice rtcDevice = this.activeCall.field.getRTCDevice(endpoint);

            // 更新数据
            this.activeCall.field.update(commField);

            endpoint.setField(this.activeCall.field);

            fillCommField(this.activeCall.field, true);

            if (null != rtcDevice) {
                CommFieldEndpoint target = endpoint;
                executeOnMainThread(() -> {
                    unfollow(target, new DefaultCallHandler(false) {
                        @Override
                        public void handleCall(CallRecord callRecord) {
                            notifyObservers(new ObservableEvent(MultipointCommEvent.Left, target));
                        }
                    }, null);
                });
            }
            else {
                this.activeCall.field.closeEndpoint(endpoint);

                notifyObservers(new ObservableEvent(MultipointCommEvent.Left, endpoint));
            }
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
