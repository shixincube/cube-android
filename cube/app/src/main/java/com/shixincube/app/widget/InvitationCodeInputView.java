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

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.shixincube.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 邀请码输入。
 */
public class InvitationCodeInputView extends RelativeLayout {

    private Context context;

    private int lineColorDefault = Color.parseColor("#999999");
    private int lineColorFocus = Color.parseColor("#3F8EED");

    private TextView tvCode1;
    private TextView tvCode2;
    private TextView tvCode3;
    private TextView tvCode4;
    private TextView tvCode5;
    private TextView tvCode6;

    private View vUnderline1;
    private View vUnderline2;
    private View vUnderline3;
    private View vUnderline4;
    private View vUnderline5;
    private View vUnderline6;

    private EditText etCode;

    private List<String> codes = new ArrayList<>();

    private OnInputListener onInputListener;

    public InvitationCodeInputView(Context context) {
        super(context);
        this.context = context;
        loadView();
    }

    public InvitationCodeInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        loadView();
    }

    public void setOnInputListener(OnInputListener listener) {
        this.onInputListener = listener;
    }

    public void showSoftInput() {
        // 显示数字软键盘
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (null != imm && null != etCode) {
            etCode.postDelayed(new Runnable() {
                @Override
                public void run() {
                    etCode.requestFocus();
                    imm.showSoftInput(etCode, 0);
                }
            },200);
        }
    }

    public String getAllCode() {
        StringBuilder buf = new StringBuilder();
        for (String code : this.codes) {
            buf.append(code);
        }
        return buf.toString();
    }

    private void loadView() {
        View view = LayoutInflater.from(context).inflate(R.layout.input_invitation_code, this);
        initView(view);
        initEvent();
    }

    private void initView(View view) {
        tvCode1 = (TextView) view.findViewById(R.id.tvCode1);
        tvCode2 = (TextView) view.findViewById(R.id.tvCode2);
        tvCode3 = (TextView) view.findViewById(R.id.tvCode3);
        tvCode4 = (TextView) view.findViewById(R.id.tvCode4);
        tvCode5 = (TextView) view.findViewById(R.id.tvCode5);
        tvCode6 = (TextView) view.findViewById(R.id.tvCode6);
        vUnderline1 = view.findViewById(R.id.v1);
        vUnderline2 = view.findViewById(R.id.v2);
        vUnderline3 = view.findViewById(R.id.v3);
        vUnderline4 = view.findViewById(R.id.v4);
        vUnderline5 = view.findViewById(R.id.v5);
        vUnderline6 = view.findViewById(R.id.v6);
        etCode = (EditText) view.findViewById(R.id.etCode);
    }

    private void initEvent() {
        etCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Nothing
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (null != editable && editable.length() > 0) {
                    etCode.setText("");
                    if (codes.size() < 6) {
                        codes.add(editable.toString());
                        refreshCode();
                    }
                }
            }
        });

        // 监听删除
        etCode.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyEvent.KEYCODE_DEL && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                    && codes.size() > 0) {
                    codes.remove(codes.size() - 1);
                    refreshCode();
                    return true;
                }

                return false;
            }
        });
    }

    private void refreshCode() {
        TextView[] codeViews = new TextView[] {
                tvCode1, tvCode2, tvCode3, tvCode4, tvCode5, tvCode6
        };
        View[] underViews = new View[] {
                vUnderline1, vUnderline2, vUnderline3, vUnderline4, vUnderline5, vUnderline6,
        };

        for (int i = 0; i < codeViews.length; ++i) {
            TextView tv = codeViews[i];
            tv.setText("");
            View line = underViews[i];
            line.setBackgroundColor(lineColorDefault);
        }

        for (int i = 0; i < this.codes.size(); ++i) {
            String code = this.codes.get(i);
            codeViews[i].setText(code);
        }

        if (this.codes.size() < 6) {
            underViews[this.codes.size()].setBackgroundColor(lineColorFocus);
        }
        else {
            underViews[this.codes.size() - 1].setBackgroundColor(lineColorFocus);
        }

        // 回调
        callback();
    }

    private void callback() {
        if (null == this.onInputListener) {
            return;
        }

        if (this.codes.size() == 6) {
            this.onInputListener.onFinish(this, getAllCode());
        }
        else {
            this.onInputListener.onInput(this);
        }
    }


    public interface OnInputListener {
        /**
         * 当有输入时回调。
         * @param view
         */
        void onInput(InvitationCodeInputView view);

        /**
         * 当完成所有输入时回调。
         * @param view
         * @param code
         */
        void onFinish(InvitationCodeInputView view, String code);
    }
}
