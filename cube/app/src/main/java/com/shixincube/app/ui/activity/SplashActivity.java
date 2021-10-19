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

import android.Manifest;
import android.content.Intent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.jaeger.library.StatusBarUtil;
import com.shixincube.app.R;
import com.shixincube.app.manager.AccountHelper;
import com.shixincube.app.manager.CubeServiceConnection;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.UIUtils;

import butterknife.BindView;
import cube.engine.CubeService;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.functions.Consumer;
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

    private CubeServiceConnection connection;

    private boolean valid;

    public SplashActivity() {
        super();
        this.valid = true;
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
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Boolean> emitter) throws Throwable {
                if (AccountHelper.getInstance(getApplicationContext()).checkValidToken()) {
                    valid = true;
                }
                else {
                    valid = false;
                }

                emitter.onNext(valid);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean valid) throws Throwable {
                if (!valid.booleanValue() && null != loginButton && null != registerButton) {
                    loginButton.setVisibility(View.VISIBLE);
                    registerButton.setVisibility(View.VISIBLE);
                }

                if (valid.booleanValue()) {
                    jumpToActivityAndClearTask(MainActivity.class);
                }
            }
        });

        // 创建引擎服务
        this.connection = new CubeServiceConnection(getApplicationContext());
        Intent intent = new Intent(this, CubeService.class);
        startService(intent);
        bindService(intent, this.connection, BIND_AUTO_CREATE);
    }
}
