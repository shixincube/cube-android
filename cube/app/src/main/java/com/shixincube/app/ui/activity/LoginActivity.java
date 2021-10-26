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
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.presenter.LoginPresenter;
import com.shixincube.app.ui.view.LoginView;
import com.shixincube.app.util.UIUtils;

import butterknife.BindView;

/**
 * 登录。
 */
public class LoginActivity extends BaseActivity<LoginView, LoginPresenter> implements LoginView {

    @BindView(R.id.etPhoneNumber)
    EditText phoneNumberText;
    @BindView(R.id.vLinePhoneNumber)
    View phoneNumberLine;

    @BindView(R.id.etPassword)
    EditText passwordText;
    @BindView(R.id.vLinePassword)
    View passwordLine;

    @BindView(R.id.btnLogin)
    Button loginButton;

    private TextWatcher watcher;

    public LoginActivity() {
        super();

        this.watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
                loginButton.setEnabled(canLogin());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };
    }

    @Override
    public void initView() {
        setToolbarTitle(getString(R.string.title_login));
    }

    @Override
    public void initListener() {
        this.phoneNumberText.addTextChangedListener(this.watcher);
        this.passwordText.addTextChangedListener(this.watcher);

        this.phoneNumberText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                phoneNumberLine.setBackgroundColor(UIUtils.getColor(R.color.theme_blue_dark));
            }
            else {
                phoneNumberLine.setBackgroundColor(UIUtils.getColor(R.color.line));
            }
        });

        this.passwordText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                passwordLine.setBackgroundColor(UIUtils.getColor(R.color.theme_blue_dark));
            }
            else {
                passwordLine.setBackgroundColor(UIUtils.getColor(R.color.line));
            }
        });

        loginButton.setOnClickListener((view) -> {
            presenter.login();
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.exit_to_bottom);
    }

    private boolean canLogin() {
        int phoneNumberLength = this.phoneNumberText.getText().toString().trim().length();
        if (phoneNumberLength < 11) {
            return false;
        }

        int passwordLength = this.passwordText.getText().toString().trim().length();
        if (passwordLength < 6) {
            return false;
        }

        return true;
    }

    @Override
    protected LoginPresenter createPresenter() {
        return new LoginPresenter(this);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_login;
    }

    @Override
    public EditText getPhoneNumberText() {
        return this.phoneNumberText;
    }

    @Override
    public EditText getPasswordText() {
        return this.passwordText;
    }
}
