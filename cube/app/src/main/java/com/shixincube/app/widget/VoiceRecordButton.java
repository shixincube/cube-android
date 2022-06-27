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

package com.shixincube.app.widget;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.shixincube.app.R;
import com.shixincube.app.util.FileUtils;
import com.shixincube.app.util.UIUtils;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import cube.engine.util.SimpleMediaRecorder;
import cube.util.LogUtils;

/**
 * 录音按钮。
 */
public class VoiceRecordButton extends androidx.appcompat.widget.AppCompatButton {

    private final static String TAG = "VoiceRecordButton";

    /**
     * 不在录音
     */
    private static final int RECORD_OFF = 0;
    /**
     * 正在录音
     */
    private static final int RECORD_ON = 1;

    /**
     * 最短有效记录时长，单位：秒。
     */
    public static final int MIN_RECORD_TIME = 1;

    /**
     * 最长记录时长，单位：秒。
     */
    public static final int MAX_RECORD_TIME = 60;

    private TextView recordTimeView;
    private TextView tipTextView;
    private ImageView recordVolumeView;

    private int recordState = RECORD_OFF;

    private float startY;

    private int volumeLevel = 0;
    private float recordTime = 0;

    private boolean isCancel = false;

    private Dialog dialog;

    private AtomicBoolean spinning;

    private SimpleMediaRecorder recorder;

    private Runnable refreshWorker;

    private OnRecordListener listener;

    public VoiceRecordButton(Context context) {
        super(context);
    }

    public VoiceRecordButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VoiceRecordButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnRecordListener(OnRecordListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (this.recordState == RECORD_OFF) {
                    showVoiceDialog();

                    this.isCancel = false;
                    this.recordState = RECORD_ON;
                    this.startY = event.getY();

                    this.startRecorder();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (this.recordState == RECORD_ON) {
                    if (this.dialog.isShowing()) {
                        this.dialog.dismiss();
                    }
                    this.recordState = RECORD_OFF;

                    this.stopRecorder();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (null != this.recorder) {
                    float moveY = event.getY();
                    float delta = this.startY - moveY;
                    if (delta > 50) {
                        this.isCancel = true;
                        this.showCancelDialog();
                    }
                    else if (delta < 20) {
                        this.isCancel = false;
                        this.showVoiceDialog();
                    }
                }
                break;
            default:
                break;
        }
        return true;
    }

    private void showVoiceDialog() {
        if (null == this.dialog) {
            this.dialog = new Dialog(getContext(), R.style.FullDialog);
            this.dialog.setContentView(R.layout.dialog_record_voice);
            this.dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            this.dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);

            this.recordTimeView = (TextView) dialog.findViewById(R.id.recordTime);
            this.tipTextView = (TextView) dialog.findViewById(R.id.tipText);
            this.recordVolumeView = (ImageView) dialog.findViewById(R.id.recordVolume);
        }

        this.tipTextView.setText(UIUtils.getString(R.string.release_to_send));
        refreshRecordTime();
        refreshVolumeImage();

        this.dialog.show();
    }

    private void showCancelDialog() {
        if (null == this.dialog) {
            return;
        }

        this.recordVolumeView.setImageResource(R.mipmap.ic_volume_cancel);
        this.tipTextView.setText(UIUtils.getString(R.string.release_to_cancel));
    }

    private void showWarningDialog() {
        if (null == this.dialog) {
            return;
        }

        this.recordVolumeView.setImageResource(R.mipmap.ic_volume_wraning);
        this.tipTextView.setText(UIUtils.getString(R.string.tip_recording_no_permission));
        this.recordTimeView.setText("");

        this.dialog.show();
    }

    private void startRecorder() {
        if (null == this.spinning) {
            this.spinning = new AtomicBoolean(false);
        }

        if (null == this.refreshWorker) {
            this.refreshWorker = new Runnable() {
                @Override
                public void run() {
                    refreshRecordTime();

                    if (isCancel) {
                        showCancelDialog();
                    }
                    else {
                        refreshVolumeImage();
                    }
                }
            };
        }

        if (null == this.recorder) {
            String dir = FileUtils.getDir("cube_files/cache/");
            String filePath = dir + "voice_" + System.currentTimeMillis() + ".m4a";

            this.recorder = new SimpleMediaRecorder(filePath);
            if (!this.recorder.start()) {
                this.recorder = null;
                // 显示警告
                this.showWarningDialog();

                if (null != this.listener) {
                    this.listener.onRecordPermissionDenied();
                }
                return;
            }

            PollingThread thread = new PollingThread();
            thread.start();
        }
    }

    private void stopRecorder() {
        if (null == this.recorder) {
            // 发生错误，回调取消
            if (null != this.listener) {
                this.listener.onRecordCancel(-1);
            }
            return;
        }

        String filePath = this.recorder.getFilePath();
        this.recorder.stop();

        if (Math.floor(this.recordTime) <= MIN_RECORD_TIME) {
            this.isCancel = true;
            UIUtils.showToast(UIUtils.getString(R.string.tip_recording_time_too_short));
        }

        if (this.isCancel) {
            File file = new File(filePath);
            LogUtils.d(TAG, "#stopRecorder - Cancel, delete file: " + file.getAbsolutePath());
            file.delete();
        }

        if (null != this.listener) {
            if (this.isCancel) {
                this.listener.onRecordCancel(Math.round(this.recordTime));
            }
            else {
                this.listener.onRecordFinish(filePath, Math.round(this.recordTime));
            }
        }

        this.recordTime = 0.0f;
        this.recorder = null;
    }

    public void release() {
        if (this.recordState == RECORD_ON) {
            this.recordState = RECORD_OFF;
        }

        if (null != this.dialog) {
            if (this.dialog.isShowing()) {
                this.dialog.dismiss();
            }
        }

        this.isCancel = true;
        stopRecorder();
    }

    private void refreshRecordTime() {
        int time = (int) this.recordTime;
        this.recordTimeView.setText(formatLongToTimeString(time));

        if (time == MAX_RECORD_TIME - 10) {
            UIUtils.showToast(UIUtils.getString(R.string.tip_recording_time_too_long));
        }
        else if (time >= MAX_RECORD_TIME) {
            UIUtils.showToast(UIUtils.getString(R.string.tip_recording_timeout));
            this.release();
        }
    }

    private void refreshVolumeImage() {
        switch (this.volumeLevel) {
            case 0:
                this.recordVolumeView.setImageResource(R.mipmap.ic_volume_0);
                break;
            case 1:
                this.recordVolumeView.setImageResource(R.mipmap.ic_volume_1);
                break;
            case 2:
                this.recordVolumeView.setImageResource(R.mipmap.ic_volume_2);
                break;
            case 3:
                this.recordVolumeView.setImageResource(R.mipmap.ic_volume_3);
                break;
            case 4:
                this.recordVolumeView.setImageResource(R.mipmap.ic_volume_4);
                break;
            case 5:
                this.recordVolumeView.setImageResource(R.mipmap.ic_volume_5);
                break;
            case 6:
                this.recordVolumeView.setImageResource(R.mipmap.ic_volume_6);
                break;
            case 7:
                this.recordVolumeView.setImageResource(R.mipmap.ic_volume_7);
                break;
            case 8:
                this.recordVolumeView.setImageResource(R.mipmap.ic_volume_8);
                break;
            default:
                break;
        }
    }

    private class PollingThread extends Thread {
        protected PollingThread() {
            super();
        }

        @Override
        public void run() {
            spinning.set(true);
            recordTime = 0.0f;

            while (recordState == RECORD_ON) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 更新时长
                recordTime += 0.1f;

                if (!isCancel) {
                    if (null != recorder) {
                        volumeLevel = Math.round(recorder.getVoiceLevel());
                    }
                }

                UIUtils.getMainThreadHandler().post(refreshWorker);
            }

            spinning.set(false);
        }
    }

    protected static String formatLongToTimeString(int second) {
        int minute = 0;
        if (second > 60) {
            minute = second / 60;
            second = second % 60;
        }
        String strMinute = "";
        if (minute > 0) {
            strMinute = (minute > 9 ? minute + "′" : "0" + minute) + "′";
        }
        return strMinute + (second > 9 ? second + "″" : "0" + second + "″");
    }

    /**
     * 录音监听器。
     */
    public interface OnRecordListener {

        /**
         * 当无法获取麦克风权限时回调。
         */
        void onRecordPermissionDenied();

        /**
         * 当录音取消时回调。
         *
         * @param durationInSeconds 已录制的时长。
         */
        void onRecordCancel(int durationInSeconds);

        /**
         * 当录音完成时回调。
         *
         * @param filePath 录音文件路径。
         * @param durationInSeconds 录音时长。
         */
        void onRecordFinish(String filePath, int durationInSeconds);
    }
}
