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

package com.shixincube.app.ui.activity;

import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.shixincube.app.CubeApp;
import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;

import butterknife.BindView;

/**
 * 文本输入界面。
 */
public class TextInputActivity extends BaseActivity {

    @BindView(R.id.tvTitle)
    TextView titleText;

    @BindView(R.id.btnCancel)
    Button cancelButton;

    @BindView(R.id.btnSubmit)
    Button submitButton;

    @BindView(R.id.etContent)
    EditText contentEditText;

    private String preloadContent = "";

    /**
     * 允许输入的最小长度。
     */
    private int minLength = 0;

    public TextInputActivity() {
        super();
    }

    @Override
    public void initView() {
        String title = getIntent().getStringExtra("title");
        if (TextUtils.isEmpty(title)) {
            this.titleText.setText("");
        }
        else {
            this.titleText.setText(title);
        }

        String content = getIntent().getStringExtra("content");
        if (TextUtils.isEmpty(content)) {
            this.contentEditText.setText("");
            this.submitButton.setEnabled(false);
        }
        else {
            this.contentEditText.setText(content);
            this.submitButton.setEnabled(true);
            this.preloadContent = content;
        }

        boolean multiline = getIntent().getBooleanExtra("multiline", false);
        if (multiline) {
            contentEditText.setMaxLines(20);
        }

        this.minLength = getIntent().getIntExtra("minLength", 0);

        this.contentEditText.requestFocus();
        CubeApp.getMainThreadHandler().postDelayed(() -> {
            contentEditText.requestFocus();
        }, 1000);
    }

    @Override
    public void initListener() {
        this.cancelButton.setOnClickListener((view) -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        this.submitButton.setOnClickListener((view) -> {
            Intent data = new Intent();
            data.putExtra("content", contentEditText.getText().toString());
            data.putExtra("preloadContent", preloadContent);
            setResult(RESULT_OK, data);
            finish();
        });

        this.contentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start,int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start,int count, int after) {
                String text = contentEditText.getText().toString();
                if (!TextUtils.isEmpty(text) && text.length() >= minLength) {
                    if (!submitButton.isEnabled()) {
                        submitButton.setEnabled(true);
                    }
                }
                else {
                    if (submitButton.isEnabled()) {
                        submitButton.setEnabled(false);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_text_input;
    }
}
