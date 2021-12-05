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

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.shixincube.app.CubeApp;
import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.fragment.FragmentFactory;
import com.shixincube.app.util.UIUtils;

import butterknife.BindView;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.filestorage.handler.DefaultDirectoryHandler;
import cube.filestorage.model.Directory;

/**
 * 创建新目录。
 */
public class NewDirectoryActivity extends BaseActivity {

    @BindView(R.id.tvTitle)
    TextView titleText;

    @BindView(R.id.btnCancel)
    Button cancelButton;

    @BindView(R.id.btnSubmit)
    Button submitButton;

    @BindView(R.id.tvTips)
    TextView tipsText;

    @BindView(R.id.etContent)
    EditText contentEditText;

    private Directory directory;

    private int minLength = 1;
    private int maxLength = 128;

    public NewDirectoryActivity() {
        super();
    }

    @Override
    public void initView() {
        this.titleText.setText(UIUtils.getString(R.string.file_new_dir));
        this.tipsText.setVisibility(View.VISIBLE);
        this.tipsText.setText(UIUtils.getString(R.string.please_enter_folder_name));

        this.submitButton.setEnabled(false);

        CubeApp.getMainThreadHandler().postDelayed(() -> {
            contentEditText.requestFocus();
        }, 500);
    }

    @Override
    public void initData() {
        this.directory = FragmentFactory.getInstance().getFilesFragment().getPresenter().getCurrentDirectory();
    }

    public void initListener() {
        this.cancelButton.setOnClickListener((view) -> {
            finish();
        });

        this.submitButton.setOnClickListener((view) -> {
            submitButton.setEnabled(false);

            showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

            this.directory.newDirectory(contentEditText.getText().toString(), new DefaultDirectoryHandler(true) {
                @Override
                public void handleDirectory(Directory directory) {
                    hideWaitingDialog();
                    finish();
                }
            }, new DefaultFailureHandler(true) {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    hideWaitingDialog();
                    UIUtils.showToast(UIUtils.getString(R.string.operate_failure_with_code, error.code));
                    finish();
                }
            });
        });

        this.contentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start,int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start,int count, int after) {
                String text = contentEditText.getText().toString();
                if (!TextUtils.isEmpty(text) && text.length() >= minLength && text.length() <= maxLength) {
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
