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

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.List;

import cube.core.handler.FailureHandler;
import cube.multipointcomm.handler.RTCDeviceHandler;
import cube.multipointcomm.util.MediaConstraint;

/**
 * RTC 设备。
 */
public class RTCDevice {

    public final static String MODE_RECEIVE_ONLY = "recvonly";
    public final static String MODE_SEND_ONLY = "sendonly";
    public final static String MODE_BIDIRECTION = "sendrecv";

    private final Context context;

    private long sn;

    /** 模式。 */
    private String mode;

    private MediaConstraint mediaConstraint;

    private List<PeerConnection.IceServer> iceServers;

    private PeerConnectionObserver pcObserver;

    private CameraVideoEventsHandler cameraVideoHandler;
    private VideoCapturer videoCapturer;
    private SurfaceViewRenderer localView;

    private PeerConnectionFactory factory;
    private EglBase.Context eglBaseContext;

    private PeerConnection pc;

    private MediaStream localMedia;

    public RTCDevice(Context context, String mode, PeerConnectionFactory factory, EglBase.Context eglBaseContext) {
        this.sn = cell.util.Utils.generateUnsignedSerialNumber();
        this.context = context;
        this.mode = mode;
        this.factory = factory;
        this.eglBaseContext = eglBaseContext;
    }

    public void openOffer(MediaConstraint mediaConstraint, RTCDeviceHandler successHandler, FailureHandler failureHandler) {
        if (null != this.pc) {
            return;
        }

        this.mediaConstraint = mediaConstraint;

        // 捕获摄像头
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread",
                this.eglBaseContext);
        this.videoCapturer = this.createCameraCapturer(true);
        VideoSource videoSource = this.factory.createVideoSource(this.videoCapturer.isScreencast());
        this.videoCapturer.initialize(surfaceTextureHelper, this.context, videoSource.getCapturerObserver());
        this.videoCapturer.startCapture(this.mediaConstraint.getVideoDimension().height,
                this.mediaConstraint.getVideoDimension().width, this.mediaConstraint.getVideoFps());

        // 设置本地摄像头的渲染界面
        this.localView.setMirror(true);
        this.localView.init(this.eglBaseContext, null);

        // 创建 Video Track
        VideoTrack videoTrack = this.factory.createVideoTrack("100", videoSource);
        videoTrack.addSink(this.localView);

        this.localMedia = this.factory.createLocalMediaStream("ARDAMS");
        this.localMedia.addTrack(videoTrack);

        this.pcObserver = new PeerConnectionObserver();
        this.pc = this.factory.createPeerConnection(this.iceServers, this.pcObserver);
        this.pc.addStream(this.localMedia);
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

        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {

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
}
