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

import com.shixincube.app.R;

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

    private Dialog dialog;

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
                }
                break;
            case MotionEvent.ACTION_UP:
                if (this.recordState == RECORD_ON) {
                    if (this.dialog.isShowing()) {
                        this.dialog.dismiss();
                    }

                    recordState = RECORD_OFF;
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
        }

        this.dialog.show();
    }
}
