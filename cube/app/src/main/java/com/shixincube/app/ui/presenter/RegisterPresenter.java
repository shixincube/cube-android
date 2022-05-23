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
import com.shixincube.app.ui.view.RegisterView;
import com.shixincube.app.util.DeviceUtils;
import com.shixincube.app.util.HashUtils;
import com.shixincube.app.util.RegularUtils;
import com.shixincube.app.util.UIUtils;

import java.util.Timer;
import java.util.TimerTask;

import cube.util.LogUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 账号注册。
 */
public class RegisterPresenter extends BasePresenter<RegisterView> {

    private Disposable disposable;

    private Timer timer;
    private int timeCountdown;

    public RegisterPresenter(BaseActivity context) {
        super(context);
    }

    /**
     * 注册账号。
     */
    public void register() {
        String nickname = getView().getNickNameEditText().getText().toString().trim();
        String phoneNumber = getView().getPhoneNumberEditText().getText().toString().trim();
        String password = getView().getPasswordEditText().getText().toString().trim();
        String code = getView().getVerificationCodeEditText().getText().toString().trim();

        if (nickname.length() < 3) {
            UIUtils.showToast(UIUtils.getString(R.string.nickname_length_error));
            return;
        }

        if (TextUtils.isEmpty(phoneNumber)) {
            UIUtils.showToast(UIUtils.getString(R.string.phone_not_empty));
            return;
        }

        if (password.length() < 6) {
            UIUtils.showToast(UIUtils.getString(R.string.password_length_error));
            return;
        }

        if (TextUtils.isEmpty(code)) {
            UIUtils.showToast(UIUtils.getString(R.string.vcode_not_empty));
            return;
        }

        // 生成密码 MD5 码
        final String passwordMD5 = HashUtils.makeMD5(password);

        Explorer.getInstance().registerAccount(phoneNumber, passwordMD5, nickname, code)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(registerResponse -> {
                if (registerResponse.code == StateCode.Success) {
                    // 注册成功，使用注册信息登录
                    login(phoneNumber, passwordMD5);
                }
                else {
                    UIUtils.showToast(UIUtils.getString(
                            R.string.register_failed_with_code, registerResponse.code));
                }
            }, this::registerError);
    }

    private void registerError(Throwable throwable) {
        UIUtils.showToast(throwable.getLocalizedMessage());
    }

    /**
     * 验证号码并发送验证码。
     */
    public void sendVerificationCode() {
        String phoneNumber = getView().getPhoneNumberEditText().getText().toString().trim();
        if (TextUtils.isEmpty(phoneNumber)) {
            UIUtils.showToast(UIUtils.getString(R.string.phone_not_empty));
            return;
        }

        // 号码格式
        if (!RegularUtils.isMobile(phoneNumber)) {
            UIUtils.showToast(UIUtils.getString(R.string.phone_format_error));
            return;
        }

        this.activity.showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

        Explorer.getInstance().checkPhoneAvailable(AppConsts.REGION_CODE, phoneNumber, true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(checkPhoneResponse -> {
                    activity.hideWaitingDialog();

                    if (checkPhoneResponse.code == StateCode.Success) {
                        // 号码可用，并已发送验证码
                        changeSendCodeButton();
                    }
                    else {
                        // 号码不可用
                        sendVerificationCodeError(new Exception(UIUtils.getString(R.string.phone_not_available)));
                    }
                }, this::sendVerificationCodeError);
    }

    private void sendVerificationCodeError(Throwable throwable) {
        this.activity.hideWaitingDialog();
        LogUtils.e(throwable.getLocalizedMessage());
        UIUtils.showToast(throwable.getLocalizedMessage());
    }

    private void changeSendCodeButton() {
        getView().getSendCodeButton().setEnabled(false);

        this.disposable = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Integer> emitter) throws Throwable {
                timeCountdown = 60;
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        emitter.onNext(--timeCountdown);
                    }
                };
                timer = new Timer();
                timer.schedule(task, 0, 1000);
            }
        }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Consumer<Integer>() {
                @Override
                public void accept(Integer time) throws Throwable {
                    if (null != getView().getSendCodeButton() && !getView().getSendCodeButton().isEnabled()) {
                        if (time >= 0) {
                            getView().getSendCodeButton().setText("重新发送 (" + time + "秒)");
                        }
                        else {
                            getView().getSendCodeButton().setEnabled(true);
                            getView().getSendCodeButton().setText(R.string.send_verification_code);
                        }
                    }
                    else {
                        timer.cancel();
                        timer = null;
                    }
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) throws Throwable {
                    LogUtils.pe(throwable);
                }
            });
    }

    public void dispose() {
        if (null != this.disposable) {
            this.disposable.dispose();
            this.disposable = null;
        }

        if (null != this.timer) {
            this.timer.cancel();
            this.timer = null;
        }
    }

    private void login(String phoneNumber, String passwordMD5Code) {
        this.activity.showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

        String device = DeviceUtils.getDeviceDescription(this.activity.getApplicationContext());

        // 登录
        Explorer.getInstance().login(phoneNumber, passwordMD5Code, device)
                .flatMap(new Function<LoginResponse, ObservableSource<AccountInfoResponse>>() {
                    @Override
                    public ObservableSource<AccountInfoResponse> apply(LoginResponse loginResponse)
                            throws Throwable {
                        if (loginResponse.code == StateCode.Success) {
                            // 保存令牌
                            AccountHelper.getInstance(activity.getApplicationContext())
                                    .saveToken(loginResponse.token, loginResponse.expire);
                            // 使用令牌获取账号信息
                            return Explorer.getInstance().getAccountInfo(loginResponse.token);
                        }
                        else {
                            return Observable.error(
                                    new Exception(UIUtils.getString(R.string.login_error)));
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

    private void loginError(Throwable throwable) {
        this.activity.hideWaitingDialog();
        LogUtils.e(throwable.getLocalizedMessage());
        UIUtils.showToast(throwable.getLocalizedMessage());
    }
}
