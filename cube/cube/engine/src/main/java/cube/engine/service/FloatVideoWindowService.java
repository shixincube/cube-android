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
import android.graphics.Point;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import cube.contact.model.Contact;
import cube.contact.model.Group;
import cube.engine.CubeEngine;
import cube.engine.R;
import cube.engine.ui.AdvancedImageView;
import cube.engine.util.ScreenUtil;
import cube.multipointcomm.util.MediaConstraint;

/**
 * 悬浮的视频窗口。
 */
public class FloatVideoWindowService extends Service {

    private final static String TAG = "FloatVideoWindowService";

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;

    private View displayView;
    private LinearLayout fullLayout;
    private RelativeLayout previewLayout;

    private AdvancedImageView avatarView;
    private TextView nameView;
    private TextView tipsView;
    private ImageButton hangupButton;
    private ImageButton microphoneButton;
    private ImageButton speakerButton;

    private Contact contact;
    private Group group;
    private MediaConstraint mediaConstraint;

    public class InnerBinder extends Binder {
        public FloatVideoWindowService getService() {
            return FloatVideoWindowService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new InnerBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.initWindow();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showFloatingWindow();
        loadData(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 初始化基本参数。
     */
    private void initWindow() {
        this.windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        this.layoutParams = getParams();
    }

    private void showFloatingWindow() {
        if (Settings.canDrawOverlays(this)) {
            if (null == this.displayView) {
                // 获取布局
                LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
                displayView = inflater.inflate(R.layout.cube_float_comm_window_layout, null);
                displayView.setOnTouchListener(new FloatingOnTouchListener());

                previewLayout = displayView.findViewById(R.id.previewLayout);
                fullLayout = displayView.findViewById(R.id.fullLayout);

                this.initView();

                windowManager.addView(displayView, layoutParams);
            }
        }
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
        this.avatarView = this.fullLayout.findViewById(R.id.ivAvatar);
        this.nameView = this.fullLayout.findViewById(R.id.tvName);
        this.tipsView = this.fullLayout.findViewById(R.id.tvTips);
        this.hangupButton = this.fullLayout.findViewById(R.id.btnHangup);
        this.microphoneButton = this.fullLayout.findViewById(R.id.btnMicrophone);
        this.speakerButton = this.fullLayout.findViewById(R.id.btnSpeaker);

        this.hangupButton.setOnClickListener((view) -> {
            CubeEngine.getInstance().getMultipointComm().hangupCall();
            this.windowManager.removeView(this.displayView);
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
            MediaConstraint mediaConstraint = new MediaConstraint(new JSONObject(jsonString));

            if (null != this.contact) {
                int resource = intent.getIntExtra("avatarResource", 0);
                if (resource > 0) {
                    this.avatarView.setImageResource(resource);
                }

                this.nameView.setText(this.contact.getPriorityName());
            }

            this.microphoneButton.setVisibility(View.GONE);
            this.speakerButton.setVisibility(View.GONE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
            if (!previewLayout.isShown()) {
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
                    layoutParams.x -= touchCurrentX - touchStartX;
                    layoutParams.y += touchCurrentY - touchStartY;
                    windowManager.updateViewLayout(displayView, layoutParams);

                    touchStartX = touchCurrentX;
                    touchStartY = touchCurrentY;
                    break;
                case MotionEvent.ACTION_UP:
                    int mStopX = (int) event.getX();
                    int mStopY = (int) event.getY();
                    if (Math.abs(startX - mStopX) >= 1 || Math.abs(startY - mStopY) >= 1) {
                        isMove = true;
                    }
                    break;
                default:
                    break;
            }

            return isMove;
        }
    }
}
