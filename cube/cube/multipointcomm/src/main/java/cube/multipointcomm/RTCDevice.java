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
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.util.List;

import cube.core.ModuleError;
import cube.core.handler.FailureHandler;
import cube.multipointcomm.handler.OfferHandler;
import cube.multipointcomm.util.MediaConstraint;
import cube.multipointcomm.util.VideoDimension;

/**
 * RTC 设备。
 */
public class RTCDevice {

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
    private VideoCapturer videoCapturer;
    private SurfaceViewRenderer localView;

    private PeerConnectionFactory factory;
    private EglBase.Context eglBaseContext;

    private PeerConnection pc;

    private MediaStream outboundStream;

    private MediaStream inboundStream;

    public RTCDevice(Context context, String mode, PeerConnectionFactory factory, EglBase.Context eglBaseContext) {
        this.sn = cell.util.Utils.generateUnsignedSerialNumber();
        this.context = context;
        this.mode = mode;
        this.factory = factory;
        this.eglBaseContext = eglBaseContext;
    }

    public long getSN() {
        return this.sn;
    }

    public String getMode() {
        return this.mode;
    }

    public void setViewRenderer(SurfaceViewRenderer viewRenderer) {
        this.localView = viewRenderer;
    }

    public void setEventListener(RTCEventListener listener) {
        this.listener = listener;
    }

    public void enableICE(List<PeerConnection.IceServer> iceServers) {
        this.iceServers = iceServers;
    }

    public void disableICE() {
        this.iceServers = null;
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

        // 创建本地流
        this.outboundStream = this.factory.createLocalMediaStream(MEDIA_STREAM_LABEL);

        if (mediaConstraint.videoEnabled) {
            // 启动摄像机提供视频画面
            VideoTrack videoTrack = createVideoTrack(mediaConstraint.getVideoDimension(), mediaConstraint.getVideoFps());
            this.outboundStream.addTrack(videoTrack);
        }

        AudioTrack audioTrack = createAudioTrack();
        this.outboundStream.addTrack(audioTrack);

        this.pcObserver = new PeerConnectionObserver();
        // 创建 Peer Connection
        this.pc = this.factory.createPeerConnection(this.iceServers, this.pcObserver);
        this.pc.addStream(this.outboundStream);

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
            }

            @Override
            public void onSetFailure(String desc) {
                // Nothing
            }
        }, this.createRtcMediaConstraints(mediaConstraint));
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
            this.videoCapturer = null;
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
        this.localView.setMirror(true);
        this.localView.setEnableHardwareScaler(true);
        this.localView.init(this.eglBaseContext, null);

        // 创建 Video Track
        VideoTrack videoTrack = this.factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        videoTrack.addSink(this.localView);

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

    private VideoCapturer createCameraCapturer(boolean front) {
        boolean camera2Supported = Camera2Enumerator.isSupported(this.context);
        CameraEnumerator enumerator = null;
        if (camera2Supported) {
            enumerator = new Camera2Enumerator(this.context);
        }
        else {
            enumerator = new Camera1Enumerator(false);
        }

        this.cameraVideoHandler = new CameraVideoEventsHandler();

        VideoCapturer videoCapturer = null;

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

    protected class CameraVideoEventsHandler implements CameraVideoCapturer.CameraEventsHandler {

        @Override
        public void onCameraError(String s) {

        }

        @Override
        public void onCameraDisconnected() {

        }

        @Override
        public void onCameraFreezed(String s) {

        }

        @Override
        public void onCameraOpening(String s) {

        }

        @Override
        public void onFirstFrameAvailable() {

        }

        @Override
        public void onCameraClosed() {

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
}
