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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.util.ArrayList;
import java.util.List;

import cube.core.ModuleError;
import cube.core.handler.FailureHandler;
import cube.multipointcomm.handler.AnswerHandler;
import cube.multipointcomm.handler.OfferHandler;
import cube.multipointcomm.handler.RTCDeviceHandler;
import cube.multipointcomm.util.MediaConstraint;
import cube.multipointcomm.util.VideoDimension;
import cube.util.LogUtils;

/**
 * RTC 设备。
 */
public class RTCDevice {

    private final static String TAG = "RTCDevice";

    public final static String MODE_RECEIVE_ONLY = "recvonly";
    public final static String MODE_SEND_ONLY = "sendonly";
    public final static String MODE_BIDIRECTION = "sendrecv";

    public final static String MEDIA_STREAM_LABEL = "ARDAMS";
    public final static String VIDEO_TRACK_ID = "ARDAMSv0";
    public final static String AUDIO_TRACK_ID = "ARDAMSa0";

    private final Context context;

    private long sn;

    /** 模式。 */
    private String mode;

    private RTCEventListener listener;

    private MediaConstraint mediaConstraint;

    private List<PeerConnection.IceServer> iceServers;

    private PeerConnectionObserver pcObserver;

    private CameraVideoEventsHandler cameraVideoHandler;
    private CameraVideoCapturer videoCapturer;

    private SurfaceViewRenderer localVideoView;
    private SurfaceViewRenderer remoteVideoView;

    private PeerConnectionFactory factory;
    private EglBase.Context eglBaseContext;

    private PeerConnection pc;

    private MediaStream outboundStream;

    private MediaStream inboundStream;

    private StreamState streamState;

    private boolean ready = false;

    private List<IceCandidate> candidates;

    public RTCDevice(Context context, String mode, PeerConnectionFactory factory, EglBase.Context eglBaseContext) {
        this.sn = cell.util.Utils.generateUnsignedSerialNumber();
        this.context = context;
        this.mode = mode;
        this.factory = factory;
        this.eglBaseContext = eglBaseContext;
        this.iceServers = new ArrayList<>();
        this.candidates = new ArrayList<>();

        this.streamState = new StreamState();
    }

    public long getSN() {
        return this.sn;
    }

    public String getMode() {
        return this.mode;
    }

    public SurfaceViewRenderer getLocalVideoView() {
        if (null == this.localVideoView) {
            this.localVideoView = new SurfaceViewRenderer(this.context);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            this.localVideoView.setLayoutParams(lp);
        }

        return this.localVideoView;
    }

    public SurfaceViewRenderer getRemoteVideoView() {
        if (null == this.remoteVideoView) {
            this.remoteVideoView = new SurfaceViewRenderer(this.context);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            this.remoteVideoView.setLayoutParams(lp);
        }

        return this.remoteVideoView;
    }

    public void swapVideoViewTop(boolean localTop) {
        if (null == this.localVideoView || null == this.remoteVideoView) {
            return;
        }

        this.localVideoView.setVisibility(View.GONE);
        this.remoteVideoView.setVisibility(View.GONE);
        this.localVideoView.setZOrderMediaOverlay(localTop);
        this.remoteVideoView.setZOrderMediaOverlay(!localTop);
        this.localVideoView.setVisibility(View.VISIBLE);
        this.remoteVideoView.setVisibility(View.VISIBLE);
    }

    public void setEventListener(RTCEventListener listener) {
        this.listener = listener;
    }

    public void enableICE(List<PeerConnection.IceServer> iceServers) {
        this.iceServers = iceServers;
    }

    public void disableICE() {
        this.iceServers = new ArrayList<>();
    }

    /**
     * 返回出站视频是否已启用。
     *
     * @return 返回出站视频是否已启用。
     */
    public boolean outboundVideoEnabled() {
        if (null == this.outboundStream) {
            return this.streamState.output.video;
        }

        this.streamState.output.video = this.streamEnabled(this.outboundStream, "video");
        return this.streamState.output.video;
    }

    /**
     * 返回出站音频是否已启用。
     *
     * @return 返回出站音频是否已启用。
     */
    public boolean outboundAudioEnabled() {
        if (null == this.outboundStream) {
            return this.streamState.output.audio;
        }

        this.streamState.output.audio = this.streamEnabled(this.outboundStream, "audio");
        return this.streamState.output.audio;
    }

    /**
     * 启用/停用出站视频。
     *
     * @param enabled 指定是否启用。
     */
    public void enableOutboundVideo(boolean enabled) {
        this.streamState.output.video = enabled;
        this.enableStream(this.outboundStream, "video", enabled);
    }

    /**
     * 启用/停用出站音频。
     *
     * @param enabled 指定是否启用。
     */
    public void enableOutboundAudio(boolean enabled) {
        this.streamState.output.audio = enabled;
        this.enableStream(this.outboundStream, "audio", enabled);
    }

    private boolean streamEnabled(MediaStream stream, String kind) {
        if (kind.equalsIgnoreCase("video")) {
            for (VideoTrack track : stream.videoTracks) {
                return track.enabled();
            }
        }
        else if (kind.equalsIgnoreCase("audio")) {
            for (AudioTrack track : stream.audioTracks) {
                return track.enabled();
            }
        }

        return false;
    }

    private boolean enableStream(MediaStream stream, String kind, boolean enabled) {
        if (null == stream) {
            return false;
        }

        boolean result = false;

        if (kind.equalsIgnoreCase("video")) {
            for (VideoTrack track : stream.videoTracks) {
                track.setEnabled(enabled);
                result = true;
            }
        }
        else if (kind.equalsIgnoreCase("audio")) {
            for (AudioTrack track : stream.audioTracks) {
                track.setEnabled(enabled);
                result = true;
            }
        }

        return result;
    }

    private void syncStreamState(MediaStream stream, StreamStateSymbol streamStateSymbol) {
        for (VideoTrack track : stream.videoTracks) {
            track.setEnabled(streamStateSymbol.video);
        }
        for (AudioTrack track : stream.audioTracks) {
            track.setEnabled(streamStateSymbol.audio);
            track.setVolume(streamStateSymbol.calcTrackVolume());
        }
    }

    /**
     * 切换摄像头。
     */
    public void switchCamera() {
        if (null != this.videoCapturer) {
            runOnUiThread(() -> {
                videoCapturer.switchCamera(null);
            });
        }
    }

    /**
     * 当前 RTC 是否正在工作。
     * @return 如果正在通话返回 {@code true} 。
     */
    public boolean isWorking() {
        return (null != this.pc);
    }

    /**
     * 启动 RTC 终端为主叫。
     *
     * @param mediaConstraint
     * @param successHandler
     * @param failureHandler
     */
    public void openOffer(MediaConstraint mediaConstraint, OfferHandler successHandler, FailureHandler failureHandler) {
        if (null != this.pc) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.ConnRepeated.code);
            failureHandler.handleFailure(null, error);
            return;
        }

        this.mediaConstraint = mediaConstraint;

        this.pcObserver = new PeerConnectionObserver();
        // 创建 Peer Connection
        this.pc = this.factory.createPeerConnection(getConfig(this.iceServers), this.pcObserver);

        if (this.mode.equals(MODE_BIDIRECTION)) {
            // 双向流
            // 创建本地流
            this.outboundStream = this.factory.createLocalMediaStream(MEDIA_STREAM_LABEL);

            if (mediaConstraint.videoEnabled) {
                // 启动摄像机提供视频画面
                VideoTrack videoTrack = this.createVideoTrack(mediaConstraint.getVideoDimension(), mediaConstraint.getVideoFps());
                this.outboundStream.addTrack(videoTrack);
            }

            if (mediaConstraint.audioEnabled) {
                // 获取麦克风数据
                AudioTrack audioTrack = this.createAudioTrack();
                this.outboundStream.addTrack(audioTrack);
            }

            // 添加媒体流
            this.pc.addStream(this.outboundStream);
        }
        else if (this.mode.equals(MODE_SEND_ONLY)) {
            // 仅发送模式
            // 创建本地流
            this.outboundStream = this.factory.createLocalMediaStream(MEDIA_STREAM_LABEL);

            if (mediaConstraint.audioEnabled) {
                this.pc.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
                        new RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY));
                AudioTrack audioTrack = this.createAudioTrack();
                audioTrack.setEnabled(true);
                this.outboundStream.addTrack(audioTrack);
//                this.pc.addTrack(audioTrack);
            }

            if (mediaConstraint.videoEnabled) {
                this.pc.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
                        new RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY));

                VideoTrack videoTrack = this.createVideoTrack(mediaConstraint.getVideoDimension(), mediaConstraint.getVideoFps());
                videoTrack.setEnabled(true);
                this.outboundStream.addTrack(videoTrack);
//                this.pc.addTrack(videoTrack);
            }

            this.pc.addStream(this.outboundStream);
        }
        else if (this.mode.equals(MODE_RECEIVE_ONLY)) {
            // 仅接收模式
            if (mediaConstraint.audioEnabled) {
                this.pc.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
                        new RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY));
            }

            if (mediaConstraint.videoEnabled) {
                this.pc.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
                        new RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY));
            }
        }

        this.pc.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                // 创建成功，设置 SDP
                pc.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        // Nothing
                    }

                    @Override
                    public void onSetSuccess() {
                        successHandler.handleOffer(RTCDevice.this, pc.getLocalDescription());
                    }

                    @Override
                    public void onCreateFailure(String desc) {
                        // Nothing
                    }

                    @Override
                    public void onSetFailure(String desc) {
                        // 设置 SDP 错误
                        ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.LocalDescriptionFault.code);
                        error.data = desc;
                        failureHandler.handleFailure(null, error);

                        // 关闭
                        close();
                    }
                }, sessionDescription);
            }

            @Override
            public void onSetSuccess() {
                // Nothing
            }

            @Override
            public void onCreateFailure(String desc) {
                // 创建 Offer SDP 错误
                ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.CreateOfferFailed.code);
                error.data = desc;
                failureHandler.handleFailure(null, error);

                // 关闭
                close();
            }

            @Override
            public void onSetFailure(String desc) {
                // Nothing
            }
        }, this.createRtcMediaConstraints(mediaConstraint));
    }

    /**
     * 启动 RTC 终端为被叫。
     *
     * @param sessionDescription
     * @param mediaConstraint
     * @param successHandler
     * @param failureHandler
     */
    public void openAnswer(SessionDescription sessionDescription, MediaConstraint mediaConstraint,
                           AnswerHandler successHandler, FailureHandler failureHandler) {
        if (null != this.pc) {
            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.ConnRepeated.code);
            failureHandler.handleFailure(null, error);
            return;
        }

        this.mediaConstraint = mediaConstraint;

        this.pcObserver = new PeerConnectionObserver();
        // 创建 Peer Connection
        this.pc = this.factory.createPeerConnection(getConfig(this.iceServers), this.pcObserver);
        this.pc.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                // Nothing
            }

            @Override
            public void onSetSuccess() {
                runOnUiThread(() -> {
                    // 创建本地流
                    outboundStream = factory.createLocalMediaStream(MEDIA_STREAM_LABEL);

                    if (mediaConstraint.videoEnabled) {
                        // 启动摄像机提供视频画面
                        VideoTrack videoTrack = createVideoTrack(mediaConstraint.getVideoDimension(), mediaConstraint.getVideoFps());
                        outboundStream.addTrack(videoTrack);
                    }

                    AudioTrack audioTrack = createAudioTrack();
                    outboundStream.addTrack(audioTrack);

                    // 添加媒体流
                    pc.addStream(outboundStream);

                    pc.createAnswer(new SdpObserver() {
                        @Override
                        public void onCreateSuccess(SessionDescription sessionDescription) {
                            // 设置本地 SDP
                            pc.setLocalDescription(new SdpObserver() {
                                @Override
                                public void onCreateSuccess(SessionDescription sessionDescription) {
                                    // Nothing
                                }

                                @Override
                                public void onSetSuccess() {
                                    successHandler.handleAnswer(RTCDevice.this, pc.getLocalDescription());
                                    // 执行就绪
                                    doReady();
                                }

                                @Override
                                public void onCreateFailure(String desc) {
                                    // Nothing
                                }

                                @Override
                                public void onSetFailure(String desc) {
                                    ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.LocalDescriptionFault.code);
                                    error.data = desc;
                                    failureHandler.handleFailure(null, error);

                                    // 关闭
                                    close();
                                }
                            }, sessionDescription);
                        }

                        @Override
                        public void onSetSuccess() {
                            // Nothing
                        }

                        @Override
                        public void onCreateFailure(String desc) {
                            ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.CreateAnswerFailed.code);
                            error.data = desc;
                            failureHandler.handleFailure(null, error);

                            // 关闭
                            close();
                        }

                        @Override
                        public void onSetFailure(String desc) {
                            // Nothing
                        }
                    }, createRtcMediaConstraints(mediaConstraint));
                });
            }

            @Override
            public void onCreateFailure(String desc) {
                // Nothing
            }

            @Override
            public void onSetFailure(String desc) {
                ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.RemoteDescriptionFault.code);
                error.data = desc;
                failureHandler.handleFailure(null, error);

                // 关闭
                close();
            }
        }, sessionDescription);
    }

    /**
     * 主叫执行 Answer 应答。
     * @param description
     * @param successHandler
     * @param failureHandler
     */
    protected void doAnswer(SessionDescription description, RTCDeviceHandler successHandler, FailureHandler failureHandler) {
        // 设置远端 SDP
        this.pc.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                // Nothing
            }

            @Override
            public void onSetSuccess() {
                successHandler.handleRTCDevice(RTCDevice.this);
                // 执行就绪
                doReady();
            }

            @Override
            public void onCreateFailure(String sdp) {
                // Nothing
            }

            @Override
            public void onSetFailure(String sdp) {
                ModuleError error = new ModuleError(MultipointComm.NAME, MultipointCommState.RemoteDescriptionFault.code);
                error.data = RTCDevice.this;
                failureHandler.handleFailure(null, error);
            }
        }, description);
    }

    private void doReady() {
        this.ready = true;

        synchronized (this.candidates) {
            for (IceCandidate candidate : this.candidates) {
                // 添加 Ice Candidate
                this.pc.addIceCandidate(candidate);

                if (LogUtils.isDebugLevel()) {
                    LogUtils.d(TAG, "#doReady - Add candidate: " + candidate.sdpMid);
                }
            }

            this.candidates.clear();
        }

        // 同步流状态
        if (null != outboundStream) {
            runOnUiThread(() -> {
                syncStreamState(outboundStream, streamState.output);
            });
        }
    }

    protected void doCandidate(IceCandidate candidate) {
        if (null == this.pc) {
            return;
        }

        if (!this.ready) {
            synchronized (this.candidates) {
                this.candidates.add(candidate);
            }
            return;
        }

        synchronized (this.candidates) {
            if (this.candidates.size() > 0) {
                for (IceCandidate iceCandidate : this.candidates) {
                    this.pc.addIceCandidate(iceCandidate);
                }
                this.candidates.clear();
            }
        }

        this.pc.addIceCandidate(candidate);

        LogUtils.d(TAG, "#doCandidate - add candidate: " + candidate.sdpMid);
    }

    /**
     * 关闭 RTC 设备。
     */
    public void close() {
        if (null != this.inboundStream) {
            this.inboundStream.dispose();
            this.inboundStream = null;
        }

        if (null != this.outboundStream) {
            this.outboundStream.dispose();
            this.outboundStream = null;
        }

        if (null != this.videoCapturer) {
            try {
                this.videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.videoCapturer.dispose();
            this.videoCapturer = null;
        }

        if (null != this.localVideoView) {
            this.localVideoView.release();
            this.localVideoView = null;
        }
        if (null != this.remoteVideoView) {
            this.remoteVideoView.release();
            this.remoteVideoView = null;
        }

        if (null != this.pc) {
            this.pc.close();
            this.pc = null;
        }
    }

    private MediaConstraints createRtcMediaConstraints(MediaConstraint mediaConstraint) {
        MediaConstraints constraints = new MediaConstraints();

        if (mediaConstraint.audioEnabled) {
            constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        }

        if (mediaConstraint.videoEnabled) {
            constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        }
        else {
            constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
        }

        return constraints;
    }

    private VideoTrack createVideoTrack(VideoDimension videoDimension, int fps) {
        // 捕获摄像头
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread",
                this.eglBaseContext);
        this.videoCapturer = this.createCameraCapturer(true);
        VideoSource videoSource = this.factory.createVideoSource(this.videoCapturer.isScreencast());
        this.videoCapturer.initialize(surfaceTextureHelper, this.context, videoSource.getCapturerObserver());
        // Capture
        this.videoCapturer.startCapture(videoDimension.height, videoDimension.width, fps);

        // 设置本地摄像头的渲染界面
        if (null == this.localVideoView) {
            this.localVideoView = new SurfaceViewRenderer(this.context);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            this.localVideoView.setLayoutParams(lp);
        }
        this.localVideoView.init(this.eglBaseContext, null);
        this.localVideoView.setMirror(true);
        this.localVideoView.setEnableHardwareScaler(true);
        this.localVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);

        // 创建 Video Track
        VideoTrack videoTrack = this.factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        videoTrack.addSink(this.localVideoView);

        return videoTrack;
    }

    private AudioTrack createAudioTrack() {
        MediaConstraints audioConstraints = new MediaConstraints();
        // 回声消除
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        // 噪声抑制
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        // 自动增益控制
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "false"));
        // 高通滤波器
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));

        AudioSource audioSource = this.factory.createAudioSource(audioConstraints);
        WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
        WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);
        AudioTrack audioTrack = this.factory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        return audioTrack;
    }

    private CameraVideoCapturer createCameraCapturer(boolean front) {
        boolean camera2Supported = Camera2Enumerator.isSupported(this.context);
        CameraEnumerator enumerator = null;
        if (camera2Supported) {
            enumerator = new Camera2Enumerator(this.context);
        }
        else {
            enumerator = new Camera1Enumerator(false);
        }

        this.cameraVideoHandler = new CameraVideoEventsHandler();

        CameraVideoCapturer videoCapturer = null;

        final String[] deviceNames = enumerator.getDeviceNames();
        for (String deviceName : deviceNames) {
            if (front ? enumerator.isFrontFacing(deviceName) : enumerator.isBackFacing(deviceName)) {
                videoCapturer = enumerator.createCapturer(deviceName, this.cameraVideoHandler);
            }

            if (null != videoCapturer) {
                break;
            }
        }

        return videoCapturer;
    }

    private PeerConnection.RTCConfiguration getConfig(List<PeerConnection.IceServer> iceServers) {
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        // 关闭分辨率变换
        rtcConfig.enableCpuOveruseDetection = false;
        rtcConfig.audioJitterBufferFastAccelerate = true;
        // 修改模式 PlanB 无法使用仅接收音视频的配置
//        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        return rtcConfig;
    }

    private void runOnUiThread(Runnable task) {
        android.os.Handler handler = new android.os.Handler(this.context.getMainLooper());
        handler.post(task);
    }

    protected class CameraVideoEventsHandler implements CameraVideoCapturer.CameraEventsHandler {

        @Override
        public void onCameraError(String deviceName) {
            if (LogUtils.isDebugLevel()) {
                LogUtils.d(TAG, "#onCameraError - " + deviceName);
            }
        }

        @Override
        public void onCameraDisconnected() {
            if (LogUtils.isDebugLevel()) {
                LogUtils.d(TAG, "#onCameraDisconnected");
            }
        }

        @Override
        public void onCameraFreezed(String deviceName) {
            if (LogUtils.isDebugLevel()) {
                LogUtils.d(TAG, "#onCameraFreezed - " + deviceName);
            }
        }

        @Override
        public void onCameraOpening(String deviceName) {
            if (LogUtils.isDebugLevel()) {
                LogUtils.d(TAG, "#onCameraOpening - " + deviceName);
            }
        }

        @Override
        public void onFirstFrameAvailable() {
            if (LogUtils.isDebugLevel()) {
                LogUtils.d(TAG, "#onFirstFrameAvailable");
            }
        }

        @Override
        public void onCameraClosed() {
            if (LogUtils.isDebugLevel()) {
                LogUtils.d(TAG, "#onCameraClosed");
            }
        }
    }

    protected class PeerConnectionObserver implements PeerConnection.Observer {

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {

        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                RTCDevice.this.listener.onMediaConnected(RTCDevice.this);
            }
            else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED ||
                    iceConnectionState == PeerConnection.IceConnectionState.FAILED ||
                    iceConnectionState == PeerConnection.IceConnectionState.CLOSED) {
                RTCDevice.this.listener.onMediaDisconnected(RTCDevice.this);
            }
        }

        @Override
        public void onIceConnectionReceivingChange(boolean changed) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            RTCDevice.this.listener.onIceCandidate(iceCandidate, RTCDevice.this);
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            if (LogUtils.isDebugLevel()) {
                LogUtils.d(TAG, "#onAddStream");
            }

            inboundStream = mediaStream;

            VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
            runOnUiThread(() -> {
                if (null == remoteVideoView) {
                    remoteVideoView = new SurfaceViewRenderer(context);
                    ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
                    remoteVideoView.setLayoutParams(lp);
                }
                remoteVideoView.init(eglBaseContext, null);
                remoteVideoView.setMirror(false);
                remoteVideoView.setEnableHardwareScaler(true);
                remoteVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);

                remoteVideoTrack.addSink(remoteVideoView);

                // 同步流状态
                syncStreamState(mediaStream, streamState.input);
            });
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {

        }

        @Override
        public void onRenegotiationNeeded() {

        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
            if (LogUtils.isDebugLevel()) {
                LogUtils.d(TAG, "#onAddTrack");
            }
        }
    }


    /**
     * 事件监听器。
     */
    public interface RTCEventListener {

        void onIceCandidate(IceCandidate iceCandidate, RTCDevice rtcDevice);

        void onMediaConnected(RTCDevice rtcDevice);

        void onMediaDisconnected(RTCDevice rtcDevice);
    }


    /**
     * 预置的流状态。
     */
    public class StreamState {

        public StreamStateSymbol input = new StreamStateSymbol();

        public StreamStateSymbol output = new StreamStateSymbol();

        protected StreamState() {
        }
    }

    /**
     * 流状态符号。
     */
    public class StreamStateSymbol {

        public boolean video = true;
        public boolean audio = true;

        /**
         * 语音 Volume 设置，取值范围 0 - 100
         */
        public int volume = 100;

        protected StreamStateSymbol() {
        }

        protected StreamStateSymbol(int volume) {
            this.volume = volume;
        }

        private double calcTrackVolume() {
            return ((double) this.volume) / 10.0f;
        }
    }
}
