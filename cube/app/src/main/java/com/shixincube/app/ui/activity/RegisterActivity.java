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

import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.presenter.RegisterPresenter;
import com.shixincube.app.ui.view.RegisterView;
import com.shixincube.app.util.UIUtils;

import butterknife.BindView;

public class RegisterActivity extends BaseActivity<RegisterView, RegisterPresenter> implements RegisterView {

    @BindView(R.id.etNickname)
    EditText nickNameText;
    @BindView(R.id.vLineNickname)
    View nickNameLine;

    @BindView(R.id.etPhoneNumber)
    EditText phoneNumberText;

    private TextWatcher watcher;

    public RegisterActivity() {
        super();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.exit_to_bottom);
    }

    @Override
    public void initListener() {
        nickNameText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                nickNameLine.setBackgroundColor(UIUtils.getColor(R.color.theme_blue_dark));
            }
            else {
                nickNameLine.setBackgroundColor(UIUtils.getColor(R.color.line));
            }
        });
    }

    @Override
    protected RegisterPresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_register;
    }

    @Override
    public EditText getNickNameEditText() {
        return null;
    }

    @Override
    public EditText getPhoneEditText() {
        return null;
    }

    @Override
    public EditText getPasswordEditText() {
        return null;
    }

    @Override
    public EditText getVerificationCodeEditText() {
        return null;
    }

    @Override
    public Button getSendCodeButtong() {
        return null;
    }
}
