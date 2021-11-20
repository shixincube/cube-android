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

import com.jaeger.library.StatusBarUtil;
import com.shixincube.app.R;
import com.shixincube.app.api.Explorer;
import com.shixincube.app.manager.AccountHelper;
import com.shixincube.app.manager.CubeConnection;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.DeviceUtils;
import com.shixincube.app.util.UIUtils;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import cube.engine.CubeService;
import cube.engine.util.Future;
import cube.engine.util.Promise;
import cube.engine.util.PromiseFuture;
import cube.engine.util.PromiseHandler;
import cube.util.LogUtils;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kr.co.namee.permissiongen.PermissionGen;

/**
 * 开屏页。
 */
public class SplashActivity extends BaseActivity {

    @BindView(R.id.accountLayout)
    RelativeLayout accountView;

    @BindView(R.id.btnLogin)
    Button loginButton;

    @BindView(R.id.btnRegister)
    Button registerButton;

    private CubeConnection connection;

    private boolean valid = true;

    private AtomicBoolean engineStarted = new AtomicBoolean(false);

    private boolean jumpToMain = false;

    public SplashActivity() {
        super();
    }

    @Override
    public void init() {
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

        // 启动
        this.launch();
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
            jumpToActivity(LoginActivity.class);
            overridePendingTransition(R.anim.entry_from_bottom, 0);
        });

        this.registerButton.setOnClickListener(v -> {
            jumpToActivity(RegisterActivity.class);
            overridePendingTransition(R.anim.entry_from_bottom, 0);
        });
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
                    if (!jumpToMain) {
                        if (valid.booleanValue() && engineStarted.get()) {
                            jumpToMain = true;
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
            engineStarted.set(true);
            synchronized (SplashActivity.this) {
                if (!jumpToMain) {
                    if (valid) {
                        jumpToMain = true;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                jumpToActivityAndClearTask(MainActivity.class);
                            }
                        });
                    }
                }
            }
        });
        intent = new Intent(this, CubeService.class);
        bindService(intent, this.connection, BIND_AUTO_CREATE);
    }

    private void login(String tokenCode) {
        String device = DeviceUtils.getDeviceDescription(this.getApplicationContext());
        Explorer.getInstance().login(tokenCode, device)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(accountInfoResponse -> {
                    LogUtils.i(SplashActivity.class.getSimpleName(),
                            "Login expire: " + (new Date(accountInfoResponse.expire).toString()));
                });
    }
}
