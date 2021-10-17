/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Shixin Cube Team.
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
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.presenter.RegisterPresenter;
import com.shixincube.app.ui.view.RegisterView;
import com.shixincube.app.util.UIUtils;

import butterknife.BindView;

/**
 * 注册账号。
 */
public class RegisterActivity extends BaseActivity<RegisterView, RegisterPresenter> implements RegisterView {

    @BindView(R.id.etNickname)
    EditText nickNameText;
    @BindView(R.id.vLineNickname)
    View nickNameLine;

    @BindView(R.id.etPhoneNumber)
    EditText phoneNumberText;
    @BindView(R.id.vLinePhoneNumber)
    View phoneNumberLine;

    @BindView(R.id.etPassword)
    EditText passwordText;
    @BindView(R.id.ivSeePwd)
    ImageView seePassword;
    @BindView(R.id.vLinePassword)
    View passwordLine;

    @BindView(R.id.etVerificationCode)
    EditText verificationCodeText;
    @BindView(R.id.btnSendCode)
    Button sendCodeButton;
    @BindView(R.id.vLineVerificationCode)
    View verificationCodeLine;

    @BindView(R.id.btnRegister)
    Button registerButton;

    private TextWatcher watcher;

    public RegisterActivity() {
        super();

        this.watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
                registerButton.setEnabled(canRegister());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };
    }

    private boolean canRegister() {
        int nickNameLength = nickNameText.getText().toString().trim().length();
        if (nickNameLength < 3) {
            return false;
        }

        int phoneNumberLength = phoneNumberText.getText().toString().trim().length();
        if (phoneNumberLength != 11) {
            return false;
        }

        int passwordLength = passwordText.getText().toString().trim().length();
        if (passwordLength < 6) {
            return false;
        }

        int verificationCodeLength = verificationCodeText.getText().toString().trim().length();
        if (verificationCodeLength < 4) {
            return false;
        }

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.presenter.dispose();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.exit_to_bottom);
    }

    @Override
    public void initView() {
        setToolbarTitle(getString(R.string.title_register));
    }

    @Override
    public void initListener() {
        this.nickNameText.addTextChangedListener(this.watcher);
        this.phoneNumberText.addTextChangedListener(this.watcher);
        this.passwordText.addTextChangedListener(this.watcher);
        this.verificationCodeText.addTextChangedListener(this.watcher);

        this.nickNameText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                nickNameLine.setBackgroundColor(UIUtils.getColor(R.color.theme_blue_dark));
            }
            else {
                nickNameLine.setBackgroundColor(UIUtils.getColor(R.color.line));
            }
        });
        this.phoneNumberText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                phoneNumberLine.setBackgroundColor(UIUtils.getColor(R.color.theme_blue_dark));
            }
            else {
                phoneNumberLine.setBackgroundColor(UIUtils.getColor(R.color.line));
            }
        });
        this.passwordText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                passwordLine.setBackgroundColor(UIUtils.getColor(R.color.theme_blue_dark));
            }
            else {
                passwordLine.setBackgroundColor(UIUtils.getColor(R.color.line));
            }
        });
        this.verificationCodeText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                verificationCodeLine.setBackgroundColor(UIUtils.getColor(R.color.theme_blue_dark));
            }
            else {
                verificationCodeLine.setBackgroundColor(UIUtils.getColor(R.color.line));
            }
        });

        seePassword.setOnClickListener(v -> {
            if (passwordText.getTransformationMethod() == HideReturnsTransformationMethod.getInstance()) {
                passwordText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            else {
                passwordText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            }

            passwordText.setSelection(passwordText.getText().toString().trim().length());
        });

        sendCodeButton.setOnClickListener(v -> {
            if (sendCodeButton.isEnabled()) {
                presenter.sendVerificationCode();
            }
        });

        registerButton.setOnClickListener(v -> {
            presenter.register();
        });
    }

    @Override
    protected RegisterPresenter createPresenter() {
        return new RegisterPresenter(this);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_register;
    }

    @Override
    public EditText getNickNameEditText() {
        return this.nickNameText;
    }

    @Override
    public EditText getPhoneNumberEditText() {
        return this.phoneNumberText;
    }

    @Override
    public EditText getPasswordEditText() {
        return this.passwordText;
    }

    @Override
    public EditText getVerificationCodeEditText() {
        return this.verificationCodeText;
    }

    @Override
    public Button getSendCodeButton() {
        return this.sendCodeButton;
    }
}
