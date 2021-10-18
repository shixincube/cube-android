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

import com.shixincube.app.AppConsts;
import com.shixincube.app.R;
import com.shixincube.app.api.Explorer;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.RegisterView;
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
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RegisterPresenter extends BasePresenter<RegisterView> {

    private Disposable disposable;

    private Timer timer;
    private int timeCountdown;

    public RegisterPresenter(BaseActivity context) {
        super(context);
    }

    public void register() {

    }

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

        this.activity.showWaitingDialog(UIUtils.getString(R.string.please_waiting));

        Explorer.getInstance().checkPhoneAvailable(AppConsts.REGION_CODE, phoneNumber, true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(checkPhoneResponse -> {
                    activity.hideWaitingDialog();

                    changeSendCodeButton();
                }, this::sendVerificationCodeError);
    }

    private void sendVerificationCodeError(Throwable throwable) {
        this.activity.hideWaitingDialog();
        LogUtils.e(throwable.getLocalizedMessage());
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
                            getView().getSendCodeButton().setText("已发送 (" + time + " 秒)");
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
}
