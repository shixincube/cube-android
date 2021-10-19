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

package com.shixincube.app.ui.presenter;

import android.text.TextUtils;

import com.shixincube.app.R;
import com.shixincube.app.api.Explorer;
import com.shixincube.app.api.StateCode;
import com.shixincube.app.manager.AccountHelper;
import com.shixincube.app.ui.activity.MainActivity;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.LoginView;
import com.shixincube.app.util.DeviceUtils;
import com.shixincube.app.util.HashUtils;
import com.shixincube.app.util.RegularUtils;
import com.shixincube.app.util.UIUtils;

import cube.util.LogUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 账号登录。
 */
public class LoginPresenter extends BasePresenter<LoginView> {

    public LoginPresenter(BaseActivity activity) {
        super(activity);
    }

    public void login() {
        String phoneNumber = getView().getPhoneNumberText().getText().toString().trim();
        String password = getView().getPasswordText().getText().toString().trim();

        if (TextUtils.isEmpty(phoneNumber)) {
            UIUtils.showToast(UIUtils.getString(R.string.phone_not_empty));
            return;
        }

        // 号码格式
        if (!RegularUtils.isMobile(phoneNumber)) {
            UIUtils.showToast(UIUtils.getString(R.string.phone_format_error));
            return;
        }

        activity.showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

        // 密码 MD5
        password = HashUtils.makeMD5(password);

        String device = DeviceUtils.getDeviceDescription(this.activity.getApplicationContext());
        Explorer.getInstance().login(phoneNumber, password, device)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(loginResponse -> {
                    activity.hideWaitingDialog();

                    if (loginResponse.code == StateCode.Success) {
                        AccountHelper.getInstance(activity.getApplicationContext()).saveToken(loginResponse.token, loginResponse.expire);
                        activity.jumpToActivityAndClearTask(MainActivity.class);
                        activity.finish();
                    }
                    else {
                        loginError(new Exception(UIUtils.getString(R.string.login_error)));
                    }
                }, this::loginError);
    }

    private void loginError(Throwable throwable) {
        this.activity.hideWaitingDialog();
        LogUtils.e(throwable.getLocalizedMessage());
        UIUtils.showToast(throwable.getLocalizedMessage());
    }
}
