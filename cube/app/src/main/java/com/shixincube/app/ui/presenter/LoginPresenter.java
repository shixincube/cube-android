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

package com.shixincube.app.ui.presenter;

import android.text.TextUtils;

import com.shixincube.app.AppConsts;
import com.shixincube.app.CubeApp;
import com.shixincube.app.R;
import com.shixincube.app.api.Explorer;
import com.shixincube.app.api.StateCode;
import com.shixincube.app.manager.AccountHelper;
import com.shixincube.app.model.Account;
import com.shixincube.app.model.response.AccountInfoResponse;
import com.shixincube.app.model.response.LoginResponse;
import com.shixincube.app.ui.activity.MainActivity;
import com.shixincube.app.ui.activity.SplashActivity;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.LoginView;
import com.shixincube.app.util.DeviceUtils;
import com.shixincube.app.util.HashUtils;
import com.shixincube.app.util.RegularUtils;
import com.shixincube.app.util.UIUtils;

import cube.util.LogUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 账号登录。
 */
public class LoginPresenter extends BasePresenter<LoginView> {

    public final static int LOGIN_BY_PHONE = 1;

    public final static int LOGIN_BY_ACCOUNT = 2;

    public LoginPresenter(BaseActivity activity) {
        super(activity);
    }

    public void login(int mode) {
        if (mode == LOGIN_BY_PHONE) {
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
            // 登录
            Explorer.getInstance().loginByPhone(phoneNumber, password, device)
                    .flatMap(new Function<LoginResponse, ObservableSource<AccountInfoResponse>>() {
                        @Override
                        public ObservableSource<AccountInfoResponse> apply(LoginResponse loginResponse) throws Throwable {
                            if (loginResponse.code == StateCode.Success) {
                                // 保存令牌
                                AccountHelper.getInstance(activity.getApplicationContext())
                                        .saveToken(loginResponse.token, loginResponse.expire);
                                // 使用令牌获取账号信息
                                return Explorer.getInstance().getAccountInfo(loginResponse.token);
                            }
                            else {
                                return Observable.error(new Exception(UIUtils.getString(R.string.login_error)));
                            }
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(accountInfoResponse -> {
                        activity.hideWaitingDialog();

                        // 账号
                        Account account = accountInfoResponse.toAccount();
                        AccountHelper.getInstance(activity.getApplicationContext())
                                .setCurrentAccount(account);

                        if (AppConsts.FERRY_MODE) {
                            CubeApp.getMainThreadHandler().post(() -> {
                                // 跳转到闪屏，以便从闪屏连接引擎
                                activity.jumpToActivityAndClearTask(SplashActivity.class);
                            });
                        }
                        else {
                            activity.jumpToActivityAndClearTask(MainActivity.class);
                        }
                    }, this::loginError);
        }
        else if (mode == LOGIN_BY_ACCOUNT) {
            String accountName = getView().getAccountText().getText().toString().trim();
            String password = getView().getAccountPasswordText().getText().toString().trim();

            activity.showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

            // 密码 MD5
            password = HashUtils.makeMD5(password);

            String device = DeviceUtils.getDeviceDescription(this.activity.getApplicationContext());
            // 登录
            Explorer.getInstance().loginByAccount(accountName, password, device)
                    .flatMap(new Function<LoginResponse, ObservableSource<AccountInfoResponse>>() {
                        @Override
                        public ObservableSource<AccountInfoResponse> apply(LoginResponse loginResponse) throws Throwable {
                            if (loginResponse.code == StateCode.Success) {
                                // 保存令牌
                                AccountHelper.getInstance(activity.getApplicationContext())
                                        .saveToken(loginResponse.token, loginResponse.expire);
                                // 使用令牌获取账号信息
                                return Explorer.getInstance().getAccountInfo(loginResponse.token);
                            }
                            else {
                                return Observable.error(new Exception(UIUtils.getString(R.string.login_error)));
                            }
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(accountInfoResponse -> {
                        activity.hideWaitingDialog();

                        // 账号
                        Account account = accountInfoResponse.toAccount();
                        AccountHelper.getInstance(activity.getApplicationContext())
                                .setCurrentAccount(account);

                        if (AppConsts.FERRY_MODE) {
                            CubeApp.getMainThreadHandler().post(() -> {
                                // 跳转到闪屏，以便从闪屏连接引擎
                                activity.jumpToActivityAndClearTask(SplashActivity.class);
                            });
                        }
                        else {
                            activity.jumpToActivityAndClearTask(MainActivity.class);
                        }
                    }, this::loginError);
        }
    }

    private void loginError(Throwable throwable) {
        this.activity.hideWaitingDialog();
        LogUtils.e(throwable.getLocalizedMessage());
        UIUtils.showToast(throwable.getLocalizedMessage());
    }
}
