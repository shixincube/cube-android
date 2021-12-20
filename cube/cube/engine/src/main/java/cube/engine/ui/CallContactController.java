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

package cube.engine.ui;

import android.content.res.Resources;
import android.media.AudioManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Timer;

import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;
import cube.engine.R;
import cube.engine.service.FloatingVideoWindowService;
import cube.engine.util.ScreenUtil;
import cube.multipointcomm.RTCDevice;
import cube.multipointcomm.handler.DefaultCommFieldHandler;
import cube.multipointcomm.model.CallRecord;
import cube.multipointcomm.model.CommField;
import cube.multipointcomm.util.MediaConstraint;

/**
 * 联系人通话控制器。
 */
public class CallContactController implements Controller {

    private FloatingVideoWindowService service;
    private AudioManager audioManager;

    private ViewGroup mainLayout;

    private RelativeLayout backboardLayout;
    private RelativeLayout foreboardLayout;
    private RelativeLayout primaryVideoContainer;
    private RelativeLayout secondaryVideoContainer;

    private LinearLayout headerLayout;
    private LinearLayout bodyLayout;
    private LinearLayout footerLayout;

    private ImageButton previewButton;
    private AdvancedImageView avatarView;
    private AdvancedImageView typeView;
    private TextView nameView;
    private TextView tipsView;
    private Button hangupButton;

    private LinearLayout microphoneLayout;
    private Button microphoneButton;
    private TextView microphoneText;

    private LinearLayout speakerLayout;
    private Button speakerButton;
    private TextView speakerText;

    private LinearLayout cameraLayout;
    private Button cameraButton;
    private Button switchCameraButton;

    private MediaConstraint mediaConstraint;

    private Timer callingTimer;

    public CallContactController(FloatingVideoWindowService service, AudioManager audioManager, ViewGroup mainLayout) {
        this.service = service;
        this.audioManager = audioManager;
        this.mainLayout = mainLayout;

        this.initView();
        this.initListener();
    }

    /**
     * 重置界面元素。
     */
    @Override
    public void reset() {
        int barHeight = ScreenUtil.getStatusBarHeight(this.service);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(this.backboardLayout.getLayoutParams());
        lp.setMargins(0, barHeight, 0, 0);
        this.backboardLayout.setLayoutParams(lp);
        this.backboardLayout.setVisibility(View.GONE);

        lp = new RelativeLayout.LayoutParams(this.foreboardLayout.getLayoutParams());
        lp.setMargins(0, barHeight, 0, 0);
        this.foreboardLayout.setLayoutParams(lp);
        this.foreboardLayout.setVisibility(View.GONE);

        this.microphoneLayout.setVisibility(View.GONE);
        this.speakerLayout.setVisibility(View.GONE);
        this.cameraLayout.setVisibility(View.GONE);
        this.cameraButton.setVisibility(View.GONE);
        this.switchCameraButton.setVisibility(View.GONE);

        this.tipsView.setVisibility(View.VISIBLE);
        this.tipsView.setText(getResources().getString(R.string.calling));
        this.hangupButton.setEnabled(true);
    }

    @Override
    public ViewGroup getMainLayout() {
        return this.mainLayout;
    }

    @Override
    public boolean isShown() {
        return this.mainLayout.isShown();
    }

    public int hideWithAnimation() {
        int duration = 300;
        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 1);
        animation.setDuration(duration);
        this.mainLayout.startAnimation(animation);
        return duration;
    }

    public void config(MediaConstraint mediaConstraint) {
        this.mediaConstraint = mediaConstraint;

        if (mediaConstraint.videoEnabled) {
            this.avatarView.setVisibility(View.GONE);
            this.nameView.setVisibility(View.GONE);
            this.cameraLayout.setVisibility(View.VISIBLE);

            this.backboardLayout.setVisibility(View.VISIBLE);
            this.foreboardLayout.setVisibility(View.VISIBLE);

            // 设置视频容器
            CubeEngine.getInstance().getMultipointComm().setRemoteVideoContainer(this.primaryVideoContainer);
            CubeEngine.getInstance().getMultipointComm().setLocalVideoContainer(this.secondaryVideoContainer);
        }
        else {
            this.avatarView.setVisibility(View.VISIBLE);
            this.nameView.setVisibility(View.VISIBLE);
            this.cameraLayout.setVisibility(View.GONE);
        }
    }

    public void startCallTiming() {
        if (this.mediaConstraint.videoEnabled) {
            this.tipsView.setVisibility(View.GONE);
        }
        else {

        }
    }

    public void stopCallTiming() {
        if (this.mediaConstraint.videoEnabled) {
            this.tipsView.setVisibility(View.VISIBLE);
        }
        else {

        }
    }

    public void changeSize(boolean mini, int widthInPixel, int heightInPixel) {
        if (mini) {
            if (this.mediaConstraint.videoEnabled) {
                this.headerLayout.setVisibility(View.GONE);
                this.footerLayout.setVisibility(View.GONE);
                this.bodyLayout.setVisibility(View.GONE);

                this.foreboardLayout.setVisibility(View.GONE);

                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(this.backboardLayout.getLayoutParams());
                lp.setMargins(0, 0, 0, 0);
                this.backboardLayout.setLayoutParams(lp);
            }
            else {
                this.headerLayout.setVisibility(View.GONE);
                this.footerLayout.setVisibility(View.GONE);
                this.nameView.setVisibility(View.GONE);
                this.tipsView.setVisibility(View.GONE);

                ViewGroup.LayoutParams params = this.avatarView.getLayoutParams();
                params.width = widthInPixel;
                params.height = heightInPixel;

                this.typeView.setVisibility(View.VISIBLE);
            }

            this.mainLayout.setBackgroundResource(R.drawable.shape_frame);
        }
        else {
            this.mainLayout.setBackgroundResource(R.mipmap.window_background);

            if (this.mediaConstraint.videoEnabled) {
                this.headerLayout.setVisibility(View.VISIBLE);
                this.footerLayout.setVisibility(View.VISIBLE);
                this.bodyLayout.setVisibility(View.VISIBLE);

                this.foreboardLayout.setVisibility(View.VISIBLE);

                int barHeight = ScreenUtil.getStatusBarHeight(this.service);
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(this.backboardLayout.getLayoutParams());
                lp.setMargins(0, barHeight, 0, 0);
                this.backboardLayout.setLayoutParams(lp);
            }
            else {
                this.headerLayout.setVisibility(View.VISIBLE);
                this.footerLayout.setVisibility(View.VISIBLE);
                this.nameView.setVisibility(View.VISIBLE);
                this.tipsView.setVisibility(View.VISIBLE);

                ViewGroup.LayoutParams params = this.avatarView.getLayoutParams();
                params.width = widthInPixel;
                params.height = heightInPixel;

                this.typeView.setVisibility(View.GONE);
            }
        }
    }

    public void setAvatarImageResource(int resource) {
        this.avatarView.setImageResource(resource);
    }

    public void setNameText(String nameText) {
        this.nameView.setText(nameText);
    }

    public void setTipsText(int resourceId) {
        this.tipsView.setText(getResources().getString(resourceId));
    }

    public void setTipsText(int resourceId, Object... formatArgs) {
        this.tipsView.setText(getResources().getString(resourceId, formatArgs));
    }

    public void setTipsText(ModuleError error) {
        this.tipsView.setText(getResources().getString(R.string.on_failed_with_error, error.code));
    }

    public void showControls(CallRecord callRecord) {
        RTCDevice device = callRecord.field.getLocalDevice();

        if (this.mediaConstraint.audioEnabled) {
            // 出站音频流是否启用，即是否启用麦克风采集数据
            boolean audioEnabled = device.outboundAudioEnabled();

            this.microphoneButton.setSelected(audioEnabled);
            this.microphoneText.setText(audioEnabled ? getResources().getString(R.string.microphone_opened)
                    : getResources().getString(R.string.microphone_closed));

            boolean speakerphoneOn = this.audioManager.isSpeakerphoneOn();
            this.speakerButton.setSelected(speakerphoneOn);
            this.speakerText.setText(speakerphoneOn ? getResources().getString(R.string.speakerphone)
                    : getResources().getString(R.string.telephone));

            this.microphoneLayout.setVisibility(View.VISIBLE);
            TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -2,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0);
            animation.setDuration(300);
            this.microphoneLayout.startAnimation(animation);

            this.speakerLayout.setVisibility(View.VISIBLE);
            animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 2,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0);
            animation.setDuration(300);
            this.speakerLayout.startAnimation(animation);
        }

        if (this.mediaConstraint.videoEnabled) {
            // 出站视频流是否可用
            boolean videoEnabled = device.outboundVideoEnabled();
            this.cameraButton.setSelected(videoEnabled);

            this.cameraButton.setVisibility(View.VISIBLE);
            TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -2,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0);
            animation.setDuration(300);
            this.cameraButton.startAnimation(animation);

            this.switchCameraButton.setVisibility(View.VISIBLE);
            animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 2,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0);
            animation.setDuration(300);
            this.switchCameraButton.startAnimation(animation);
        }
    }

    public void hideControls() {
        this.microphoneLayout.setVisibility(View.INVISIBLE);
        this.speakerLayout.setVisibility(View.INVISIBLE);
        this.cameraLayout.setVisibility(View.INVISIBLE);
    }

    /**
     * 交换两个视频界面。
     */
    public void swapVideoView() {
        View view = this.backboardLayout.getChildAt(0);
        if (view.getId() == this.primaryVideoContainer.getId()) {
            // 将副视频界面切换到背板
            this.backboardLayout.removeView(this.primaryVideoContainer);
            this.foreboardLayout.removeView(this.secondaryVideoContainer);

            CubeEngine.getInstance().getMultipointComm()
                    .getActiveCallRecord()
                    .getCommField()
                    .getLocalDevice()
                    .swapVideoViewTop(false);

            this.backboardLayout.addView(this.secondaryVideoContainer);
            this.foreboardLayout.addView(this.primaryVideoContainer);
        }
        else {
            // 将主视频界面切换到背板
            this.backboardLayout.removeView(this.secondaryVideoContainer);
            this.foreboardLayout.removeView(this.primaryVideoContainer);

            CubeEngine.getInstance().getMultipointComm()
                    .getActiveCallRecord()
                    .getCommField()
                    .getLocalDevice()
                    .swapVideoViewTop(true);

            this.backboardLayout.addView(this.primaryVideoContainer);
            this.foreboardLayout.addView(this.secondaryVideoContainer);
        }
    }

    private void initView() {
        this.backboardLayout = this.mainLayout.findViewById(R.id.rlBackboard);
        this.foreboardLayout = this.mainLayout.findViewById(R.id.rlForeboard);
        this.primaryVideoContainer = this.mainLayout.findViewById(R.id.primaryVideoContainer);
        this.secondaryVideoContainer = this.mainLayout.findViewById(R.id.secondaryVideoContainer);

        this.headerLayout = this.mainLayout.findViewById(R.id.llHeader);
        this.bodyLayout = this.mainLayout.findViewById(R.id.llBody);
        this.footerLayout = this.mainLayout.findViewById(R.id.llFooter);

        this.previewButton = this.mainLayout.findViewById(R.id.btnPreview);
        this.avatarView = this.mainLayout.findViewById(R.id.ivAvatar);
        this.typeView = this.mainLayout.findViewById(R.id.ivType);
        this.nameView = this.mainLayout.findViewById(R.id.tvName);
        this.tipsView = this.mainLayout.findViewById(R.id.tvTips);
        this.hangupButton = this.mainLayout.findViewById(R.id.btnHangup);

        this.microphoneLayout = this.mainLayout.findViewById(R.id.llMicrophone);
        this.microphoneButton = this.mainLayout.findViewById(R.id.btnMicrophone);
        this.microphoneText = this.mainLayout.findViewById(R.id.tvMicrophone);
        this.speakerLayout = this.mainLayout.findViewById(R.id.llSpeaker);
        this.speakerButton = this.mainLayout.findViewById(R.id.btnSpeaker);
        this.speakerText = this.mainLayout.findViewById(R.id.tvSpeaker);

        this.cameraLayout = this.mainLayout.findViewById(R.id.llCameraLayout);
        this.cameraButton = this.mainLayout.findViewById(R.id.btnCamera);
        this.switchCameraButton = this.mainLayout.findViewById(R.id.btnSwitchCamera);
    }

    private void initListener() {
        this.previewButton.setOnClickListener((view) -> {
            service.switchToPreview();
        });

        this.hangupButton.setOnClickListener((view) -> {
            hangupButton.setEnabled(false);
            hideControls();

            CubeEngine.getInstance().getMultipointComm().hangupCall(new DefaultCommFieldHandler(true) {
                @Override
                public void handleCommField(CommField commField) {
                    service.hide();
                }
            }, new DefaultFailureHandler(true) {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    service.hide();
                }
            });
        });

        this.microphoneButton.setOnClickListener((view) -> {
            boolean state = !microphoneButton.isSelected();
            microphoneButton.setSelected(state);
            microphoneText.setText(state ? getResources().getString(R.string.microphone_opened)
                    : getResources().getString(R.string.microphone_closed));

            // 开关麦克风
            CubeEngine.getInstance().getMultipointComm()
                    .getActiveCallRecord()
                    .getCommField()
                    .getLocalDevice()
                    .enableOutboundAudio(state);
        });

        this.speakerButton.setOnClickListener((view) -> {
            boolean speakerphoneState = audioManager.isSpeakerphoneOn();
            speakerphoneState = !speakerphoneState;
            audioManager.setSpeakerphoneOn(speakerphoneState);
            speakerButton.setSelected(speakerphoneState);
            speakerText.setText(speakerphoneState ? getResources().getString(R.string.speakerphone)
                    : getResources().getString(R.string.telephone));
        });

        this.cameraButton.setOnClickListener((view) -> {
            boolean state = !cameraButton.isSelected();
            cameraButton.setSelected(state);

            // 开关摄像机
            CubeEngine.getInstance().getMultipointComm()
                    .getActiveCallRecord()
                    .getCommField()
                    .getLocalDevice()
                    .enableOutboundVideo(state);
        });

        this.switchCameraButton.setOnClickListener((view) -> {
            CubeEngine.getInstance().getMultipointComm()
                    .getActiveCallRecord()
                    .getCommField()
                    .getLocalDevice()
                    .switchCamera();
        });

        this.foreboardLayout.setOnClickListener((view) -> {
            // 切换画面
            swapVideoView();
        });
    }

    private Resources getResources() {
        return this.service.getResources();
    }
}
