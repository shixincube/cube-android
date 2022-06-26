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
import com.shixincube.app.util.UIUtils;

/**
 * 录音按钮。
 */
public class VoiceRecordButton extends androidx.appcompat.widget.AppCompatButton {

    /**
     * 不在录音
     */
    private static final int RECORD_OFF = 0;
    /**
     * 正在录音
     */
    private static final int RECORD_ON = 1;

    private int recordState = RECORD_OFF;

    private float startY;

    private int volumeLevel = 0;

    private boolean isCancel = false;

    private Dialog dialog;

    private TextView recordTime;
    private TextView tipText;
    private ImageView recordVolume;

    public VoiceRecordButton(Context context) {
        super(context);
    }

    public VoiceRecordButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VoiceRecordButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (this.recordState == RECORD_OFF) {
                    showVoiceDialog();
                    this.recordState = RECORD_ON;
                    this.startY = event.getY();

                    this.startRecorder();
                    this.isCancel = false;
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

            this.recordTime = (TextView) dialog.findViewById(R.id.recordTime);
            this.tipText = (TextView) dialog.findViewById(R.id.tipText);
            this.recordVolume = (ImageView) dialog.findViewById(R.id.recordVolume);
        }

        this.tipText.setText(UIUtils.getString(R.string.sliding_up_to_cancel));
        refreshVolumeImage();

        this.dialog.show();
    }

    private void showCancelDialog() {
        if (null == this.dialog) {
            return;
        }

        this.recordVolume.setImageResource(R.mipmap.ic_volume_cancel);
        this.tipText.setText(UIUtils.getString(R.string.release_to_cancel));
    }

    private void startRecorder() {

    }

    private void stopRecorder() {

    }

    private void refreshVolumeImage() {
        switch (this.volumeLevel) {
            case 0:
                this.recordVolume.setImageResource(R.mipmap.ic_volume_0);
                break;
            default:
                break;
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
}
