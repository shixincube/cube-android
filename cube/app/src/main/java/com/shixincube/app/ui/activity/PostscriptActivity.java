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
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.UIUtils;

import butterknife.BindView;

/**
 * 附言界面。
 */
public class PostscriptActivity extends BaseActivity {

    @BindView(R.id.etPostscript)
    EditText postscriptEdit;

    @BindView(R.id.ibClear)
    ImageButton clearEditText;

    public PostscriptActivity() {
        super();
    }

    @Override
    public void initView() {
        this.toolbarFuncButton.setVisibility(View.VISIBLE);
        this.toolbarFuncButton.setText(UIUtils.getString(R.string.send));
        this.toolbarFuncButton.setEnabled(false);
    }

    @Override
    public void initListener() {
        this.postscriptEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
                String text = postscriptEdit.getText().toString().trim();
                if (!TextUtils.isEmpty(text)) {
                    toolbarFuncButton.setEnabled(true);
                }
                else {
                    toolbarFuncButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        this.clearEditText.setOnClickListener((view) -> {
            postscriptEdit.setText("");
        });

        this.toolbarFuncButton.setOnClickListener((view) -> {
            Intent intent = new Intent();
            intent.putExtra("postscript", postscriptEdit.getText().toString());
            setResult(RESULT_OK, intent);
            finish();
        });
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_postscript;
    }
}
