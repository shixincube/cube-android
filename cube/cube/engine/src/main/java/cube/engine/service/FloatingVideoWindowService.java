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

package cube.engine.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.MutableInt;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import cube.contact.model.Contact;
import cube.contact.model.Group;
import cube.core.ModuleError;
import cube.engine.CubeEngine;
import cube.engine.R;
import cube.engine.ui.AdvancedImageView;
import cube.engine.ui.KeyEventLinearLayout;
import cube.engine.util.ScreenUtil;
import cube.multipointcomm.CallListener;
import cube.multipointcomm.model.CallRecord;
import cube.multipointcomm.util.MediaConstraint;
import cube.util.LogUtils;

/**
 * 悬浮的视频窗口。
 */
public class FloatingVideoWindowService extends Service implements KeyEventLinearLayout.KeyEventListener, CallListener {

    private final static String TAG = "FloatVideoWindowService";

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;

    private KeyEventLinearLayout displayView;
    private LinearLayout mainLayout;

    private LinearLayout headerLayout;
    private LinearLayout bodyLayout;
    private LinearLayout footerLayout;

    private ImageButton previewButton;
    private AdvancedImageView avatarView;
    private AdvancedImageView typeView;
    private TextView nameView;
    private TextView tipsView;
    private ImageButton hangupButton;

    private LinearLayout microphoneLayout;
    private ImageButton microphoneButton;
    private TextView microphoneText;

    private LinearLayout speakerLayout;
    private ImageButton speakerButton;
    private TextView speakerText;

    private boolean previewMode = false;

    private Contact contact;
    private Group group;
    private MediaConstraint mediaConstraint;

    private Timer callTimer;

    @Override
    public void onCreate() {
        super.onCreate();
        this.initWindow();

        CubeEngine.getInstance().getMultipointComm().addCallListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        show();
        loadData(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (null != this.callTimer) {
            this.callTimer.cancel();
            this.callTimer = null;
        }

        if (null != this.windowManager) {
            if (null != this.displayView.getParent()) {
                this.windowManager.removeViewImmediate(this.displayView);
            }
        }

        CubeEngine.getInstance().getMultipointComm().removeCallListener(this);
    }

    public class InnerBinder extends Binder {
        public FloatingVideoWindowService getService() {
            return FloatingVideoWindowService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new InnerBinder();
    }

    /**
     * 初始化基本参数。
     */
    private void initWindow() {
        this.windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        this.layoutParams = getParams();
    }

    /**
     * 显示默认界面。
     */
    private void show() {
        if (Settings.canDrawOverlays(this)) {
            if (null == this.displayView) {
                // 获取布局
                LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
                displayView = (KeyEventLinearLayout) inflater.inflate(R.layout.cube_comm_window_layout, null);
                displayView.setKeyEventListener(this);
                displayView.setOnTouchListener(new FloatingOnTouchListener());

                mainLayout = displayView.findViewById(R.id.llMainLayout);

                this.initView();
                this.initListener();
            }

            this.previewMode = false;
            windowManager.addView(displayView, layoutParams);
        }
    }

    /**
     * 从窗口删除。
     */
    private void hide() {
        if (null != this.callTimer) {
            this.callTimer.cancel();
            this.callTimer = null;
        }

        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 1);
        animation.setDuration(300);
        this.mainLayout.startAnimation(animation);

        Handler handler = new Handler(getApplicationContext().getMainLooper());
        handler.postDelayed(() -> {
            windowManager.removeView(displayView);
        }, 310);
    }

    private void switchToPreview() {
        if (this.previewMode) {
            return;
        }

        this.previewMode = true;

        this.headerLayout.setVisibility(View.GONE);
        this.footerLayout.setVisibility(View.GONE);
        this.nameView.setVisibility(View.GONE);
        this.tipsView.setVisibility(View.GONE);

        this.mainLayout.setBackgroundResource(R.drawable.shape_frame);

        ViewGroup.LayoutParams params = this.avatarView.getLayoutParams();
        params.width = ScreenUtil.dp2px(this, 50);
        params.height = ScreenUtil.dp2px(this, 50);

        this.typeView.setVisibility(View.VISIBLE);

        Point size = ScreenUtil.getScreenSize(this);

        this.layoutParams.width = ScreenUtil.dp2px(this, 55);
        this.layoutParams.height = ScreenUtil.dp2px(this, 55);
        this.layoutParams.x = size.x - this.layoutParams.width;
        this.layoutParams.y = ScreenUtil.getStatusBarHeight(this) + ScreenUtil.dp2px(this, 50);
        this.windowManager.updateViewLayout(this.displayView, this.layoutParams);
    }

    private void switchToFull() {
        if (!this.previewMode) {
            return;
        }

        this.previewMode = false;

        this.mainLayout.setBackgroundResource(R.mipmap.window_background);

        this.headerLayout.setVisibility(View.VISIBLE);
        this.footerLayout.setVisibility(View.VISIBLE);
        this.nameView.setVisibility(View.VISIBLE);
        this.tipsView.setVisibility(View.VISIBLE);

        ViewGroup.LayoutParams params = this.avatarView.getLayoutParams();
        params.width = ScreenUtil.dp2px(this, 120);
        params.height = ScreenUtil.dp2px(this, 120);

        this.typeView.setVisibility(View.GONE);

        Point size = ScreenUtil.getScreenSize(this);

        this.layoutParams.width = size.x;
        this.layoutParams.height = size.y;
        this.layoutParams.x = 0;
        this.layoutParams.y = 0;
        this.windowManager.updateViewLayout(this.displayView, this.layoutParams);
    }

    private void showControls(CallRecord callRecord) {
        if (this.mediaConstraint.audioEnabled) {
//            RTCDevice device = callRecord.field.getLocalDevice();

            boolean audioEnabled = true;
            this.microphoneButton.setSelected(audioEnabled);
            this.microphoneText.setText(audioEnabled ? getResources().getString(R.string.enabled)
                    : getResources().getString(R.string.disabled));

            this.microphoneLayout.setVisibility(View.VISIBLE);
            TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -2,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0);
            animation.setDuration(500);
            this.microphoneLayout.startAnimation(animation);

            this.speakerLayout.setVisibility(View.VISIBLE);
            animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 2,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0);
            animation.setDuration(500);
            this.speakerLayout.startAnimation(animation);
        }
    }

    private void hideControls() {
        this.microphoneButton.setVisibility(View.GONE);
        this.speakerButton.setVisibility(View.GONE);
    }

    private WindowManager.LayoutParams getParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        else {
            params.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        // 设置可显示在状态栏
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        // 透明
        params.format = PixelFormat.TRANSPARENT;

        Point size = ScreenUtil.getScreenSize(this);

        // 设置悬浮窗口宽高数据
        params.width = size.x;
        params.height = size.y;
        params.x = 0;
        params.y = 0;

        // 对齐左上角
        params.gravity = Gravity.LEFT | Gravity.TOP;

        return params;
    }

    private void initView() {
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
    }

    private void initListener() {
        this.previewButton.setOnClickListener((view) -> {
            switchToPreview();
        });

        this.hangupButton.setOnClickListener((view) -> {
            CubeEngine.getInstance().getMultipointComm().hangupCall();
        });

        this.microphoneButton.setOnClickListener((view) -> {
            boolean state = !microphoneButton.isSelected();
            microphoneButton.setSelected(state);
            microphoneText.setText(state ? getResources().getString(R.string.enabled)
                    : getResources().getString(R.string.disabled));
        });
        this.speakerButton.setOnClickListener((view) -> {
            speakerButton.setSelected(!speakerButton.isSelected());
        });
    }

    private void loadData(Intent intent) {
        if (intent.hasExtra("contactId")) {
            Long contactId = intent.getLongExtra("contactId", 0);
            this.contact = CubeEngine.getInstance().getContactService().getContact(contactId);
        }
        else if (intent.hasExtra("groupId")) {
            Long groupId = intent.getLongExtra("groupId", 0);
            this.group = CubeEngine.getInstance().getContactService().getGroup(groupId);
        }

        String jsonString = intent.getStringExtra("mediaConstraint");
        try {
            this.mediaConstraint = new MediaConstraint(new JSONObject(jsonString));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.microphoneLayout.setVisibility(View.GONE);
        this.speakerLayout.setVisibility(View.GONE);

        if (null != this.contact) {
            int resource = intent.getIntExtra("avatarResource", 0);
            if (resource > 0) {
                this.avatarView.setImageResource(resource);
            }

            this.nameView.setText(this.contact.getPriorityName());

            // 呼叫联系人
//            CubeEngine.getInstance().getMultipointComm().makeCall(this.contact, mediaConstraint, new DefaultCallHandler(true) {
//                @Override
//                public void handleCall(CallRecord callRecord) {
//                    tipsView.setText(getResources().getString(R.string.on_ringing));
//                    // 显示控件
//                    showControls(callRecord);
//                }
//            }, new DefaultFailureHandler(true) {
//                @Override
//                public void handleFailure(Module module, ModuleError error) {
//                    hide();
//                }
//            });
        }

        showControls(null);
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (!previewMode) {
                switchToPreview();
                return true;
            }
        }

        return false;
    }


    private class FloatingOnTouchListener implements View.OnTouchListener {
        /**
         * 开始触控的坐标，移动时的坐标（相对于屏幕左上角的坐标）
         */
        private int touchStartX;
        private int touchStartY;

        /**
         * 开始时的坐标和结束时的坐标（相对于自身控件的坐标）
         */
        private int startX;
        private int startY;

        /**
         * 判断悬浮窗口是否移动，这里做个标记，防止移动后松手触发了点击事件
         */
        private boolean isMove;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (!previewMode) {
                return true;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isMove = false;
                    touchStartX = (int) event.getRawX();
                    touchStartY = (int) event.getRawY();
                    startX = (int) event.getX();
                    startY = (int) event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int touchCurrentX = (int) event.getRawX();
                    int touchCurrentY = (int) event.getRawY();

                    layoutParams.x += touchCurrentX - touchStartX;
                    layoutParams.y += touchCurrentY - touchStartY;
                    windowManager.updateViewLayout(displayView, layoutParams);

                    touchStartX = touchCurrentX;
                    touchStartY = touchCurrentY;
                    break;
                case MotionEvent.ACTION_UP:
                    int stopX = (int) event.getX();
                    int stopY = (int) event.getY();
                    if (Math.abs(startX - stopX) >= 1 || Math.abs(startY - stopY) >= 1) {
                        isMove = true;
                    }
                    else {
                        switchToFull();
                    }
                    break;
                default:
                    break;
            }

            return isMove;
        }
    }

    @Override
    public void onInProgress(CallRecord callRecord) {
        if (LogUtils.isDebugLevel()) {
            LogUtils.d(TAG, "#onInProgress");
        }

        tipsView.setText(getResources().getString(R.string.on_in_progress));
    }

    @Override
    public void onRinging(CallRecord callRecord) {
        if (LogUtils.isDebugLevel()) {
            LogUtils.d(TAG, "#onRinging");
        }

        if (null == this.callTimer) {
            Handler handler = new Handler(getApplicationContext().getMainLooper());
            MutableInt count = new MutableInt(0);

            this.callTimer = new Timer();
            this.callTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handler.post(() -> {
                        count.value += 1;
                        tipsView.setText(getResources().getString(R.string.on_ringing_with_count, count.value));
                    });
                }
            }, 1000, 1000);
        }
    }

    @Override
    public void onConnected(CallRecord callRecord) {
        if (LogUtils.isDebugLevel()) {
            LogUtils.d(TAG, "#onConnected");
        }

        if (null != this.callTimer) {
            this.callTimer.cancel();
            this.callTimer = null;
        }
    }

    @Override
    public void onBusy(CallRecord callRecord) {
        if (LogUtils.isDebugLevel()) {
            LogUtils.d(TAG, "#onBusy");
        }

        if (null != this.callTimer) {
            this.callTimer.cancel();
            this.callTimer = null;
        }
    }

    @Override
    public void onBye(CallRecord callRecord) {
        if (LogUtils.isDebugLevel()) {
            LogUtils.d(TAG, "#onBye");
        }

        hide();
    }

    @Override
    public void onTimeout(CallRecord callRecord) {
        if (LogUtils.isDebugLevel()) {
            LogUtils.d(TAG, "#onTimeout");
        }

        if (null != this.callTimer) {
            this.callTimer.cancel();
            this.callTimer = null;
        }
    }

    @Override
    public void onFailed(ModuleError error) {
        if (LogUtils.isDebugLevel()) {
            LogUtils.d(TAG, "#onFailed : " + error.code);
        }

        if (null != this.callTimer) {
            this.callTimer.cancel();
            this.callTimer = null;
        }
    }
}
