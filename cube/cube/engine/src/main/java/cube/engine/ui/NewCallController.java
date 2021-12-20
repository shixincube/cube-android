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

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.Uri;
import android.provider.Settings;
import android.util.Size;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import cube.contact.model.Contact;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;
import cube.engine.R;
import cube.engine.service.FloatingVideoWindowService;
import cube.engine.util.ScreenUtil;
import cube.multipointcomm.handler.DefaultCallHandler;
import cube.multipointcomm.model.CallRecord;
import cube.multipointcomm.util.MediaConstraint;

/**
 * 新通话邀请控制器。
 */
public class NewCallController implements Controller {

    private FloatingVideoWindowService service;

    private ViewGroup mainLayout;

    private ImageView avatarView;
    private ImageView typeView;
    private TextView tipsText;

    private ImageButton hangupButton;
    private ImageButton answerButton;

    private Contact caller;

    private MediaConstraint mediaConstraint;

    private int avatarResourceId;

    public NewCallController(FloatingVideoWindowService service, ViewGroup mainLayout) {
        this.service = service;
        this.mainLayout = mainLayout;

        this.initView();
        this.initListener();
    }

    @Override
    public Size reset() {
        if (!Settings.canDrawOverlays(this.service)) {
            this.service.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + this.service.getApplicationContext().getPackageName())));
            return null;
        }

        this.hangupButton.setEnabled(true);
        this.answerButton.setEnabled(true);

        int barHeight = ScreenUtil.getStatusBarHeight(this.service);
        this.mainLayout.setPadding(0, barHeight + ScreenUtil.dp2px(this.service, 30), 0, 0);

        this.avatarView.setImageResource(R.mipmap.cube);

        Point screenSize = ScreenUtil.getScreenSize(this.service);
        Size size = new Size(screenSize.x, 1000);
        return size;
    }

    @Override
    public ViewGroup getMainLayout() {
        return this.mainLayout;
    }

    @Override
    public boolean isShown() {
        return this.mainLayout.isShown();
    }

    public void showWithAnimation(Contact caller, MediaConstraint mediaConstraint, int avatarResourceId) {
        this.caller = caller;
        this.mediaConstraint = mediaConstraint;
        this.avatarResourceId = avatarResourceId;

        if (avatarResourceId > 0) {
            this.avatarView.setImageResource(avatarResourceId);
        }
        else {
            this.avatarView.setImageResource(R.mipmap.avatar);
        }

        if (mediaConstraint.videoEnabled) {
            this.typeView.setImageResource(R.mipmap.ic_video_call);
            this.tipsText.setText(this.getResources().getString(R.string.on_new_call_video_from_contact, caller.getPriorityName()));
        }
        else {
            this.typeView.setImageResource(R.mipmap.ic_audio_call);
            this.tipsText.setText(this.getResources().getString(R.string.on_new_call_audio_from_contact, caller.getPriorityName()));
        }

        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, -1,
                Animation.RELATIVE_TO_SELF, 0);
        animation.setDuration(300);
        this.mainLayout.startAnimation(animation);
    }

    public int hideWithAnimation() {
        int duration = 300;
        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, -1);
        animation.setDuration(duration);
        this.mainLayout.startAnimation(animation);
        return duration;
    }

    private void initView() {
        this.avatarView = this.mainLayout.findViewById(R.id.ivAvatar);
        this.typeView = this.mainLayout.findViewById(R.id.ivType);
        this.tipsText = this.mainLayout.findViewById(R.id.tvTips);
        this.hangupButton = this.mainLayout.findViewById(R.id.btnHangup);
        this.answerButton = this.mainLayout.findViewById(R.id.btnAnswer);
    }

    private void initListener() {
        this.hangupButton.setOnClickListener((view) -> {
            hangupButton.setEnabled(false);
            answerButton.setEnabled(false);

            CubeEngine.getInstance().getMultipointComm().hangupCall(new DefaultCallHandler(true) {
                @Override
                public void handleCall(CallRecord callRecord) {
                    // Nothing
                }
            }, new DefaultFailureHandler(true) {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    // Nothing
                }
            });
        });

        this.answerButton.setOnClickListener((view) -> {
            answerButton.setEnabled(false);
            hangupButton.setEnabled(false);

            service.showCallee(this.caller, this.mediaConstraint, this.avatarResourceId);
        });
    }

    private Resources getResources() {
        return this.service.getResources();
    }
}
