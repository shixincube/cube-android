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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cube.contact.model.Contact;
import cube.contact.model.Group;
import cube.contact.model.Self;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;
import cube.engine.R;
import cube.engine.service.FloatingVideoWindowListener;
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

    private LinearLayout normalLayout;
    private LinearLayout minimizeLayout;

    private TextView miniCallingTimeText;

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

    private Map<Long, Long> invitationTimeMap;

    public GroupCallingController(FloatingVideoWindowService service, AudioManager audioManager, ViewGroup mainLayout) {
        this.service = service;
        this.audioManager = audioManager;
        this.mainLayout = mainLayout;
        this.invitationTimeMap = new HashMap<>();

        this.initView();
        this.initListener();
    }

    @Override
    public Size reset() {
        this.normalLayout.setVisibility(View.VISIBLE);
        this.minimizeLayout.setVisibility(View.GONE);

        this.microphoneLayout.setVisibility(View.GONE);
        this.speakerLayout.setVisibility(View.GONE);
        this.cameraLayout.setVisibility(View.GONE);
        this.cameraButton.setVisibility(View.GONE);
        this.switchCameraButton.setVisibility(View.GONE);

        this.hangupButton.setEnabled(true);

        this.callingTimeText.setVisibility(View.GONE);

        this.miniCallingTimeText.setText("");

        return null;
    }

    public void config(MediaConstraint mediaConstraint) {
        this.mediaConstraint = mediaConstraint;

        if (mediaConstraint.videoEnabled) {
            this.cameraLayout.setVisibility(View.VISIBLE);
            ImageView imageView = this.minimizeLayout.findViewById(R.id.ivType);
            imageView.setImageResource(R.mipmap.ic_video_call);
        }
        else {
            this.cameraLayout.setVisibility(View.GONE);
            ImageView imageView = this.minimizeLayout.findViewById(R.id.ivType);
            imageView.setImageResource(R.mipmap.ic_audio_call);
        }

        this.invitationTimeMap.clear();
    }

    /**
     * 启动。
     *
     * @param group
     * @param contactList
     * @param avatarResIds
     */
    public void start(Group group, List<Contact> contactList, List<Integer> avatarResIds) {
        this.group = group;
        this.members = contactList;

        for (int i = 0; i < contactList.size(); ++i) {
            Contact contact = contactList.get(i);
            this.gridLayout.showGrid(contact.getId())
                .setImageResource(avatarResIds.get(i));
            this.gridLayout.playWaiting(contact.getId());
        }
    }

    public void addParticipant(Contact contact, int avatarResId) {
        if (this.members.contains(contact)) {
            return;
        }

        this.members.add(contact);
        this.gridLayout.showGrid(contact.getId())
                .setImageResource(avatarResId);
        this.gridLayout.playWaiting(contact.getId());

        // 设置邀请时间
        this.invitationTimeMap.put(contact.id, System.currentTimeMillis());
    }

    public List<Contact> getParticipants() {
        return this.members;
    }

    public void changeSize(boolean minimum, int widthInPixel, int heightInPixel) {
        if (minimum) {
            this.normalLayout.setVisibility(View.GONE);
            this.minimizeLayout.setVisibility(View.VISIBLE);

            this.mainLayout.setBackgroundResource(R.drawable.shape_frame);
        }
        else {
            this.normalLayout.setVisibility(View.VISIBLE);
            this.minimizeLayout.setVisibility(View.GONE);

            this.mainLayout.setBackgroundResource(R.mipmap.window_background);
        }
    }

    public void setTipsText(ModuleError error) {
        this.callingTimeText.setText(getResources().getString(R.string.on_failed_with_error, error.code));
    }

    public void stopWaiting(Contact contact) {
        int index = this.members.indexOf(contact);
        if (index >= 0) {
            this.gridLayout.stopWaiting(contact.getId());
        }
    }

    public void showControls(CallRecord callRecord) {
        RTCDevice device = callRecord.field.getLocalDevice();

        if (this.mediaConstraint.audioEnabled && !this.microphoneLayout.isShown()) {
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

        if (this.mediaConstraint.videoEnabled && !this.cameraButton.isShown()) {
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
        if (this.microphoneLayout.isShown()) {
            this.microphoneLayout.setVisibility(View.INVISIBLE);
        }

        if (this.speakerLayout.isShown()) {
            this.speakerLayout.setVisibility(View.INVISIBLE);
        }

        if (this.cameraLayout.isShown()) {
            this.cameraLayout.setVisibility(View.INVISIBLE);
        }
    }

    public Runnable startCallTiming() {
        this.miniCallingTimeText.setText("");
        this.callingTimeText.setText("");
        this.callingTimeText.setVisibility(View.VISIBLE);
        this.timing = 0;

        Self self = CubeEngine.getInstance().getContactService().getSelf();
        Long timestamp = System.currentTimeMillis();
        for (Contact contact : this.members) {
            if (contact.id.longValue() == self.id.longValue()) {
                continue;
            }
            this.invitationTimeMap.put(contact.id, timestamp);
        }

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

        String text = buf.toString();
        this.callingTimeText.setText(text);
        this.miniCallingTimeText.setText(text);

        long timestamp = System.currentTimeMillis();
        Iterator<Map.Entry<Long, Long>> iter = this.invitationTimeMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, Long> entry = iter.next();
            long time = entry.getValue().longValue();
            if (timestamp - time > 10000) {
                Long id = entry.getKey();
                // 删除超时参与人
                int index = -1;
                Contact contact = null;
                for (int i = 0; i < this.members.size(); ++i) {
                    contact = this.members.get(i);
                    if (contact.id.longValue() == id.longValue()) {
                        index = i;
                        break;
                    }
                }

                if (index > -1) {
                    this.members.remove(index);
                    this.gridLayout.closeGrid(contact.getId());
                    iter.remove();
                }
            }
        }
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
            this.gridLayout.stopWaiting(endpoint.getContact().getId());
            this.gridLayout.getAvatarView(endpoint.getContact().getId()).setVisibility(View.GONE);
            ViewGroup viewGroup = this.gridLayout.getVideoContainer(endpoint.getContact().getId());
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
        this.normalLayout = this.mainLayout.findViewById(R.id.llNormal);
        this.minimizeLayout = this.mainLayout.findViewById(R.id.llMinimize);

        this.miniCallingTimeText = this.mainLayout.findViewById(R.id.tvCallingTimeMini);

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

        this.mainLayout.findViewById(R.id.btnInvite).setOnClickListener((view) -> {
            int maxNum = mediaConstraint.videoEnabled ? 6 : 9;
            if (members.size() < maxNum) {
                FloatingVideoWindowListener listener = service.getBinder().getListener();
                if (null != listener) {
                    listener.onInviteClick(view, service, group, members);
                }
            }
        });
    }

    private Resources getResources() {
        return this.service.getResources();
    }
}
