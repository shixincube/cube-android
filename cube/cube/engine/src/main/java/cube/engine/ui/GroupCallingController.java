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
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import cube.contact.model.Contact;
import cube.contact.model.Group;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;
import cube.engine.R;
import cube.engine.service.FloatingVideoWindowService;
import cube.multipointcomm.RTCDevice;
import cube.multipointcomm.VideoContainerAgent;
import cube.multipointcomm.handler.DefaultCallHandler;
import cube.multipointcomm.model.CallRecord;
import cube.multipointcomm.model.CommFieldEndpoint;
import cube.multipointcomm.util.MediaConstraint;

/**
 * 群组通话控制器。
 */
public class GroupCallingController implements Controller, Runnable, VideoContainerAgent {

    private FloatingVideoWindowService service;
    private AudioManager audioManager;

    private ViewGroup mainLayout;

    private MultipointGridLayout gridLayout;

    private TextView callingTimeText;
    private ImageButton previewButton;

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

    private int timing;

    private Group group;
    private List<Contact> members;

    public GroupCallingController(FloatingVideoWindowService service, AudioManager audioManager, ViewGroup mainLayout) {
        this.service = service;
        this.audioManager = audioManager;
        this.mainLayout = mainLayout;

        this.initView();
        this.initListener();
    }

    public void config(MediaConstraint mediaConstraint) {
        this.mediaConstraint = mediaConstraint;
    }

    public void set(Group group, List<Contact> contactList, List<Integer> avatarResIds) {
        this.group = group;
        this.members = contactList;
        this.gridLayout.setNeededCount(this.members.size());

        for (int i = 0; i < avatarResIds.size(); ++i) {
            this.gridLayout.getAvatarView(i).setImageResource(avatarResIds.get(i));
        }
    }

    public void setTipsText(ModuleError error) {
        this.callingTimeText.setText(getResources().getString(R.string.on_failed_with_error, error.code));
    }

    public void stopWaiting(Contact contact) {
        int index = this.members.indexOf(contact);
        if (index >= 0) {
            this.gridLayout.stopWaiting(index);
        }
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

            this.speakerLayout.setVisibility(View.VISIBLE);
        }
    }

    public void hideControls() {
        this.microphoneLayout.setVisibility(View.INVISIBLE);
        this.speakerLayout.setVisibility(View.INVISIBLE);
        this.cameraLayout.setVisibility(View.INVISIBLE);
    }

    public Runnable startCallTiming() {
        this.callingTimeText.setText("");
        this.callingTimeText.setVisibility(View.VISIBLE);
        this.timing = 0;
        return this;
    }

    public void stopCallTiming() {
        this.callingTimeText.setVisibility(View.GONE);
    }

    @Override
    public void run() {
        ++this.timing;

        StringBuilder buf = new StringBuilder();

        if (this.timing >= 60) {
            int minute = (int) Math.floor(this.timing / 60.0);
            int mod = this.timing % 60;

            if (minute < 10) {
                buf.append("0");
            }
            buf.append(minute);

            buf.append(":");

            if (mod < 10) {
                buf.append("0");
            }
            buf.append(mod);
        }
        else {
            buf.append("00:");
            if (this.timing < 10) {
                buf.append("0");
            }
            buf.append(this.timing);
        }

        this.callingTimeText.setText(buf.toString());
    }

    @Override
    public Size reset() {
        this.microphoneLayout.setVisibility(View.GONE);
        this.speakerLayout.setVisibility(View.GONE);
        this.cameraLayout.setVisibility(View.GONE);
        this.cameraButton.setVisibility(View.GONE);
        this.switchCameraButton.setVisibility(View.GONE);

        this.hangupButton.setEnabled(true);

        this.callingTimeText.setVisibility(View.GONE);

        return null;
    }

    @Override
    public ViewGroup getMainLayout() {
        return this.mainLayout;
    }

    @Override
    public boolean isShown() {
        return this.mainLayout.isShown();
    }

    @Override
    public ViewGroup getVideoContainer(CommFieldEndpoint endpoint) {
        int index = this.members.indexOf(endpoint.getContact());
        if (index >= 0) {
            this.gridLayout.stopWaiting(index);
            this.gridLayout.getAvatarView(index).setVisibility(View.GONE);
            ViewGroup viewGroup = this.gridLayout.getVideoContainer(index);
            viewGroup.setVisibility(View.VISIBLE);
            return viewGroup;
        }

        return null;
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

    private void initView() {
        this.gridLayout = this.mainLayout.findViewById(R.id.mglGrid);

        this.callingTimeText = this.mainLayout.findViewById(R.id.tvCallingTime);
        this.previewButton = this.mainLayout.findViewById(R.id.btnPreview);

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

            CubeEngine.getInstance().getMultipointComm().hangupCall(new DefaultCallHandler(true) {
                @Override
                public void handleCall(CallRecord callRecord) {
                    service.hide();
                }
            }, new DefaultFailureHandler(true) {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    service.hide();
                }
            });
        });
    }

    private Resources getResources() {
        return this.service.getResources();
    }
}
