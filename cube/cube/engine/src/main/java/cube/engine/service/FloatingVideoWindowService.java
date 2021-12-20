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
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.MutableInt;
import android.util.Size;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import cube.contact.model.Contact;
import cube.contact.model.Group;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;
import cube.engine.R;
import cube.engine.ui.ContactCallingController;
import cube.engine.ui.KeyEventLinearLayout;
import cube.engine.ui.NewCallController;
import cube.engine.util.ScreenUtil;
import cube.multipointcomm.CallListener;
import cube.multipointcomm.handler.DefaultCallHandler;
import cube.multipointcomm.model.CallRecord;
import cube.multipointcomm.util.MediaConstraint;
import cube.util.LogUtils;

/**
 * 悬浮的视频窗口。
 */
public class FloatingVideoWindowService extends Service implements KeyEventLinearLayout.KeyEventListener, CallListener {

    private final static String TAG = "FloatVideoWindowService";

    /**
     * 准备界面。
     */
    public final static String ACTION_PREPARE = "ACTION_PREPARE";

    /**
     * 显示主叫界面。
     */
    public final static String ACTION_SHOW_CALLER = "ACTION_SHOW_CALLER";

    /**
     * 显示被叫界面。
     */
    public final static String ACTION_SHOW_CALLEE = "ACTION_SHOW_CALLEE";

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;

    private AudioManager audioManager;

    private SoundPool soundPool;

    private KeyEventLinearLayout displayView;

    private NewCallController newCallController;
    private ContactCallingController contactCallingController;

    private FloatingVideoWindowBinder binder;

    private boolean previewMode = false;

    private Contact contact;
    private Group group;
    private MediaConstraint mediaConstraint;

    private Timer callTimer;

    private AtomicBoolean closing = new AtomicBoolean(false);

    @Override
    public void onCreate() {
        super.onCreate();

        this.audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.soundPool = new SoundPool.Builder().setMaxStreams(4).build();
        }
        else {
            this.soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
        }

        this.initWindow();

        CubeEngine.getInstance().getMultipointComm().addCallListener(this);
    }

    private void initWindow() {
        this.windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        this.layoutParams = getParams();

        if (null == this.displayView) {
            // 获取布局
            LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
            this.displayView = (KeyEventLinearLayout) inflater.inflate(R.layout.cube_comm_window_layout, null);
            this.displayView.setKeyEventListener(this);
            this.displayView.setOnTouchListener(new FloatingOnTouchListener());

            this.newCallController = new NewCallController(this,
                    this.displayView.findViewById(R.id.rlNewCallLayout));

            this.contactCallingController = new ContactCallingController(this,
                    this.audioManager, this.displayView.findViewById(R.id.rlContactCallLayout));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (null == action) {
            return super.onStartCommand(intent, flags, startId);
        }

        if (action.equals(ACTION_PREPARE)) {
            this.newCallController.getMainLayout().setVisibility(View.VISIBLE);
            this.contactCallingController.getMainLayout().setVisibility(View.GONE);
        }
        else if (action.equals(ACTION_SHOW_CALLER)) {
            showCaller(intent);
        }

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (null == this.binder) {
            this.binder = new FloatingVideoWindowBinder();
        }
        return this.binder;
    }

    /**
     * 显示联系人主叫界面并发起主叫。
     *
     * @param intent
     */
    private void showCaller(Intent intent) {
        if (null == intent) {
            return;
        }

        this.newCallController.getMainLayout().setVisibility(View.GONE);
        this.contactCallingController.getMainLayout().setVisibility(View.VISIBLE);

        Size size = this.contactCallingController.reset();
        if (null == size) {
            Point screenSize = ScreenUtil.getScreenSize(this);
            this.layoutParams.width = screenSize.x;
            this.layoutParams.height = screenSize.y;
        }

        this.previewMode = false;
        this.windowManager.addView(this.displayView, this.layoutParams);

        if (!this.startCallerByContact(intent)) {
            this.windowManager.removeView(this.displayView);
        }
    }

    /**
     * 显示联系人被叫界面并应答通话。
     *
     * @param contact
     * @param mediaConstraint
     * @param avatarResourceId
     */
    public void showCallee(Contact contact, MediaConstraint mediaConstraint, int avatarResourceId) {
        Runnable task = () -> {
            this.contactCallingController.getMainLayout().setVisibility(View.VISIBLE);
            Size size = this.contactCallingController.reset();
            if (null == size) {
                Point screenSize = ScreenUtil.getScreenSize(this);
                this.layoutParams.width = screenSize.x;
                this.layoutParams.height = screenSize.y;
            }

            this.previewMode = false;
            this.windowManager.updateViewLayout(this.displayView, this.layoutParams);

            if (!this.startCalleeByContact(contact, mediaConstraint, avatarResourceId)) {
                this.windowManager.removeView(this.displayView);
            }
        };

        if (this.newCallController.isShown()) {
            // 隐藏界面
            int delay = this.newCallController.hideWithAnimation();
            Handler handler = new Handler(getApplicationContext().getMainLooper());
            handler.postDelayed(() -> {
                newCallController.getMainLayout().setVisibility(View.GONE);
                task.run();
            }, delay);
        }
        else {
            task.run();
        }
    }

    /**
     * 显示有新的通话邀请。
     *
     * @param callRecord
     */
    private void showNewIncomingCall(CallRecord callRecord) {
        this.newCallController.getMainLayout().setVisibility(View.VISIBLE);
        this.contactCallingController.getMainLayout().setVisibility(View.GONE);

        Size size = this.newCallController.reset();
        if (null != size) {
            this.layoutParams.width = size.getWidth();
            this.layoutParams.height = size.getHeight();
        }

        this.windowManager.addView(this.displayView, this.layoutParams);

        if (callRecord.field.isPrivate()) {
            // 来自一对一通话
            Contact caller = callRecord.field.getCaller();
            int resId = 0;
            if (null != this.binder && null != this.binder.getContactDataHandler()) {
                resId = this.binder.getContactDataHandler().extractContactAvatarResourceId(caller);
            }

            this.newCallController.showWithAnimation(caller, callRecord.getCallerConstraint(), resId);
        }
        else {
            // TODO
        }
    }

    /**
     * 从窗口删除。
     */
    public synchronized void hide() {
        if (null != this.callTimer) {
            this.callTimer.cancel();
            this.callTimer = null;
        }

        if (this.closing.get()) {
            return;
        }

        this.closing.set(true);

        if (this.contactCallingController.isShown()) {
            this.contactCallingController.stopCallTiming();

            int delay = this.contactCallingController.hideWithAnimation();

            Handler handler = new Handler(getApplicationContext().getMainLooper());
            handler.postDelayed(() -> {
                windowManager.removeView(displayView);
                closing.set(false);
            }, delay);
        }
        else if (this.newCallController.isShown()) {
            int delay = this.newCallController.hideWithAnimation();

            Handler handler = new Handler(getApplicationContext().getMainLooper());
            handler.postDelayed(() -> {
                windowManager.removeView(displayView);
                closing.set(false);
            }, delay);
        }
    }

    public void switchToPreview() {
        if (this.previewMode) {
            return;
        }

        this.previewMode = true;

        if (this.contactCallingController.isShown()) {
            if (this.mediaConstraint.videoEnabled) {
                this.contactCallingController.changeSize(true,
                        ScreenUtil.dp2px(this, 80),
                        ScreenUtil.dp2px(this, 120));
            }
            else {
                this.contactCallingController.changeSize(true,
                        ScreenUtil.dp2px(this, 50),
                        ScreenUtil.dp2px(this, 50));
            }
        }

        Point size = ScreenUtil.getScreenSize(this);

        if (this.mediaConstraint.videoEnabled) {
            this.layoutParams.width = ScreenUtil.dp2px(this, 80);
            this.layoutParams.height = ScreenUtil.dp2px(this, 120);
        }
        else {
            this.layoutParams.width = ScreenUtil.dp2px(this, 55);
            this.layoutParams.height = ScreenUtil.dp2px(this, 55);
        }

        this.layoutParams.x = size.x - this.layoutParams.width;
        this.layoutParams.y = ScreenUtil.getStatusBarHeight(this) + ScreenUtil.dp2px(this, 50);
        this.windowManager.updateViewLayout(this.displayView, this.layoutParams);
    }

    public void switchToFull() {
        if (!this.previewMode) {
            return;
        }

        this.previewMode = false;

        Point size = ScreenUtil.getScreenSize(this);

        if (this.contactCallingController.isShown()) {
            if (this.mediaConstraint.videoEnabled) {
                this.contactCallingController.changeSize(false, size.x, size.y);
            }
            else {
                this.contactCallingController.changeSize(false,
                        ScreenUtil.dp2px(this, 120),
                        ScreenUtil.dp2px(this, 120));
            }
        }

        this.layoutParams.width = size.x;
        this.layoutParams.height = size.y;
        this.layoutParams.x = 0;
        this.layoutParams.y = 0;
        this.windowManager.updateViewLayout(this.displayView, this.layoutParams);
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

    private boolean startCallerByContact(Intent intent) {
        if (intent.hasExtra("contactId")) {
            Long contactId = intent.getLongExtra("contactId", 0);
            this.contact = CubeEngine.getInstance().getContactService().getContact(contactId);
        }
        else {
            return false;
        }

        String jsonString = intent.getStringExtra("mediaConstraint");
        try {
            this.mediaConstraint = new MediaConstraint(new JSONObject(jsonString));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 设置媒体约束
        this.contactCallingController.config(this.mediaConstraint);

        if (null != this.contact) {
            // 头像
            this.contactCallingController.setAvatarImageResource(intent.getIntExtra("avatarResource", 0));
            // 名字
            this.contactCallingController.setNameText(this.contact.getPriorityName());

            // 呼叫联系人
            CubeEngine.getInstance().getMultipointComm().makeCall(this.contact, mediaConstraint, new DefaultCallHandler(true) {
                @Override
                public void handleCall(CallRecord callRecord) {
                    contactCallingController.setTipsText(R.string.on_ringing);
                    // 显示控件
                    contactCallingController.showControls(callRecord);
                }
            }, new DefaultFailureHandler(true) {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    contactCallingController.setTipsText(error);
                    Handler handler = new Handler(getMainLooper());
                    handler.postDelayed(() -> {
                        hide();
                    }, 3000);
                }
            });
        }

        return true;
    }

    private boolean startCalleeByContact(Contact contact, MediaConstraint mediaConstraint, int avatarResourceId) {
        this.mediaConstraint = mediaConstraint;
        this.contact = contact;

        // 设置媒体约束
        this.contactCallingController.config(mediaConstraint);
        // 头像
        this.contactCallingController.setAvatarImageResource(avatarResourceId);
        // 名字
        this.contactCallingController.setNameText(contact.getPriorityName());
        // 提示
        this.contactCallingController.setTipsText(R.string.answering);

        // 应答
        CubeEngine.getInstance().getMultipointComm().answerCall(mediaConstraint, new DefaultCallHandler(true) {
            @Override
            public void handleCall(CallRecord callRecord) {
                // 显示控件
                contactCallingController.showControls(callRecord);
            }
        }, new DefaultFailureHandler(true) {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                contactCallingController.setTipsText(error);
                Handler handler = new Handler(getMainLooper());
                handler.postDelayed(() -> {
                    hide();
                }, 3000);
            }
        });

        return true;
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
    public void onNewCall(CallRecord callRecord) {
        if (LogUtils.isDebugLevel()) {
            LogUtils.d(TAG, "#onNewCall");
        }

        this.showNewIncomingCall(callRecord);
    }

    @Override
    public void onInProgress(CallRecord callRecord) {
        if (LogUtils.isDebugLevel()) {
            LogUtils.d(TAG, "#onInProgress");
        }

        if (this.contactCallingController.isShown()) {
            this.contactCallingController.setTipsText(R.string.on_in_progress);
        }
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
                        contactCallingController.setTipsText(R.string.on_ringing_with_count, count.value);
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

        this.contactCallingController.startCallTiming();
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

        this.contactCallingController.setTipsText(R.string.on_timeout);
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
