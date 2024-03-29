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

import android.Manifest;
import android.content.Intent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.jaeger.library.StatusBarUtil;
import com.shixincube.app.AppConsts;
import com.shixincube.app.R;
import com.shixincube.app.api.Explorer;
import com.shixincube.app.api.RetryWithDelay;
import com.shixincube.app.manager.AccountHelper;
import com.shixincube.app.manager.CubeConnection;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.DeviceUtils;
import com.shixincube.app.util.UIUtils;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import butterknife.BindView;
import cell.util.log.Logger;
import cube.core.KernelConfig;
import cube.engine.CubeEngine;
import cube.engine.service.CubeService;
import cube.engine.util.Future;
import cube.engine.util.Promise;
import cube.engine.util.PromiseFuture;
import cube.engine.util.PromiseHandler;
import cube.util.LogUtils;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kr.co.namee.permissiongen.PermissionFail;
import kr.co.namee.permissiongen.PermissionGen;
import kr.co.namee.permissiongen.PermissionSuccess;

/**
 * 开屏页。
 */
public class SplashActivity extends BaseActivity {

    private final static String TAG = SplashActivity.class.getSimpleName();

    @BindView(R.id.accountLayout)
    RelativeLayout accountView;

    @BindView(R.id.btnLogin)
    Button loginButton;

    @BindView(R.id.btnRegister)
    Button registerButton;

    private boolean permissionOk = true;

    private CubeConnection connection;

    private boolean valid = true;

    private AtomicBoolean engineStarted = new AtomicBoolean(false);

    private AtomicBoolean jumpToMain = new AtomicBoolean(false);
    private AtomicBoolean finishLogin = new AtomicBoolean(false);
    private AtomicBoolean finishGetAccount = new AtomicBoolean(false);

    public SplashActivity() {
        super();
    }

    @Override
    public void init() {
        // 请求权限
        this.requestPermission();

        if (AppConsts.FERRY_MODE) {
            AtomicInteger count = new AtomicInteger(0);
            checkEngineConfig(new ResultCallback() {
                @Override
                public void onResult(boolean ok) {
                    if (!ok) {
                        UIUtils.showToast(UIUtils.getString(R.string.network_failure), 3000);

                        if (count.incrementAndGet() >= 2) {
                            // 重试两次
                            launch();
                        }
                    }
                    else {
                        // 启动
                        launch();
                    }
                }
            });
        }
        else {
            // 启动
            this.launch();
        }
    }

    @Override
    public void initView() {
        StatusBarUtil.setColor(this, UIUtils.getColor(R.color.black));

        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
        alphaAnimation.setDuration(1000);
        this.accountView.startAnimation(alphaAnimation);

        if (!this.valid) {
            this.loginButton.setVisibility(View.VISIBLE);
            this.registerButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void initListener() {
        this.loginButton.setOnClickListener(v -> {
            if (!permissionOk) {
                requestPermission();
            }

            jumpToActivity(LoginActivity.class);
            overridePendingTransition(R.anim.entry_from_bottom, 0);
        });

        this.registerButton.setOnClickListener(v -> {
            if (!permissionOk) {
                requestPermission();
            }

            jumpToActivity(RegisterActivity.class);
            overridePendingTransition(R.anim.entry_from_bottom, 0);
        });
    }

    private void requestPermission() {
        PermissionGen.with(this)
                .addRequestCode(100)
                .permissions(
                        // 网络权限
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        // 存储权限
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_SETTINGS
                )
                .request();
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_splash;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (null != this.connection) {
            unbindService(this.connection);
            this.connection = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionGen.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @PermissionSuccess(requestCode = 100)
    public void onPermissionSuccess() {
        this.permissionOk = true;
    }

    @PermissionFail(requestCode = 100)
    public void onPermissionFail() {
        this.permissionOk = false;
    }

    private void launch() {
        // 判断是否有有效令牌
        Promise.create(new PromiseHandler<Boolean>() {
            @Override
            public void emit(PromiseFuture<Boolean> promise) {
                // 先置为 false
                valid = false;
                if (AccountHelper.getInstance(getApplicationContext()).checkValidToken()) {
                    valid = true;

                    // 使用令牌登录到应用服务器
                    login(AccountHelper.getInstance().getTokenCode());
                }
                else {
                    valid = false;
                }

                promise.resolve(valid);
            }
        }).thenOnMainThread(new Future<Boolean>() {
            @Override
            public void come(Boolean valid) {
                if (!valid.booleanValue() && null != loginButton && null != registerButton) {
                    loginButton.setVisibility(View.VISIBLE);
                    registerButton.setVisibility(View.VISIBLE);
                }

                synchronized (SplashActivity.this) {
                    if (valid.booleanValue() && !jumpToMain.get() && engineStarted.get()) {
                        jumpToMain.set(true);
                        if (AppConsts.FERRY_MODE) {
                            jumpToActivityAndClearTask(FerryActivity.class);
                        }
                        else {
                            jumpToActivityAndClearTask(MainActivity.class);
                        }
                    }
                }
            }
        }).launch();

        // 创建引擎服务
        Intent intent = new Intent(this, CubeService.class);
        intent.setAction(CubeService.ACTION_START);
        startService(intent);

        // 监听引擎启动
        this.connection = new CubeConnection();
        this.connection.setSuccessHandler(() -> {
            Logger.d(TAG, "#launch - engine started");
            engineStarted.set(true);
            synchronized (SplashActivity.this) {
                if (!jumpToMain.get() && valid) {
                    jumpToMain.set(true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (AppConsts.FERRY_MODE) {
                                jumpToActivityAndClearTask(FerryActivity.class);
                            }
                            else {
                                jumpToActivityAndClearTask(MainActivity.class);
                            }
                        }
                    });
                }
            }
        });
        intent = new Intent(this, CubeService.class);
        bindService(intent, this.connection, BIND_AUTO_CREATE);
    }

    private void login(String tokenCode) {
        String device = DeviceUtils.getDeviceDescription(this.getApplicationContext());
        // 登录到 App 服务器
        Explorer.getInstance().login(tokenCode, device)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnError(error -> {
                    LogUtils.w(TAG, "#login - login", error);
                    finishLogin.set(true);
                })
                .subscribe(loginResponse -> {
                    LogUtils.i(TAG,
                            "Login expire: " + (new Date(loginResponse.expire).toString()));
                    finishLogin.set(true);
                });

        if (jumpToMain.get()) {
            return;
        }

        // 更新账号数据
        Explorer.getInstance().getAccountInfo(tokenCode)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnError(error -> {
                    LogUtils.w(TAG, "#login - getAccountInfo", error);
                    finishGetAccount.set(true);
                })
                .subscribe(accountInfoResponse -> {
                    finishGetAccount.set(true);

                    // 更新本地数据
                    AccountHelper.getInstance().updateCurrentAccount(accountInfoResponse.name,
                            accountInfoResponse.avatar);

                    if (jumpToMain.get()) {
                        return;
                    }

                    synchronized (SplashActivity.this) {
                        if (valid && !jumpToMain.get() && engineStarted.get()) {
                            jumpToMain.set(true);
                            runOnUiThread(() -> {
                                if (AppConsts.FERRY_MODE) {
                                    jumpToActivityAndClearTask(FerryActivity.class);
                                }
                                else {
                                    jumpToActivityAndClearTask(MainActivity.class);
                                }
                            });
                        }
                    }
                });
    }

    private void checkEngineConfig(ResultCallback callback) {
        KernelConfig config = CubeEngine.getInstance().loadConfig(getApplicationContext());

        Explorer.getInstance().getDomain(config.domain, config.appKey)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnError(error -> {
                    LogUtils.w(TAG, "#checkEngineConfig", error);

                    runOnUiThread(() -> {
                        callback.onResult(false);
                    });
                })
                .retryWhen(new RetryWithDelay(3500, 2))
                .subscribe(domain -> {
                    if (!domain.mainEndpoint.host.equals("127.0.0.1")) {
                        // 跳过调试地址 127.0.0.1

                        if (!domain.mainEndpoint.host.equals(config.address) ||
                                domain.mainEndpoint.port != config.port) {
                            // 配置信息与当前信息不符
                            LogUtils.i(TAG, "Reset config file");
                            CubeEngine.getInstance().saveConfig(getApplicationContext(),
                                    domain.mainEndpoint.host, domain.mainEndpoint.port,
                                    domain.domainName, domain.appKey);
                        }
                    }

                    runOnUiThread(() -> {
                        callback.onResult(true);
                    });
                });
    }


    public abstract class ResultCallback {
        public ResultCallback() {
        }

        public abstract void onResult(boolean ok);
    }
}
