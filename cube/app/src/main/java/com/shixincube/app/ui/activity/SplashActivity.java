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
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.jaeger.library.StatusBarUtil;
import com.shixincube.app.R;
import com.shixincube.app.manager.CubeServiceConnection;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.UIUtils;

import butterknife.BindView;
import cube.engine.CubeService;
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

    public SplashActivity() {
        super();
    }

    @Override
    public void init() {
        PermissionGen.with(this)
                .addRequestCode(100)
                .permissions(
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
    }

    @Override
    public void initListener() {
        this.loginButton.setOnClickListener(v -> {

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
            Intent intent = new Intent(this, CubeService.class);
            unbindService(this.connection);
            this.connection = null;
        }
    }

    private void launch() {
        // 判断是否有有效令牌
        // TODO

        this.connection = new CubeServiceConnection(getApplicationContext());
        Intent intent = new Intent(this, CubeService.class);
        startService(intent);
        bindService(intent, this.connection, BIND_AUTO_CREATE);
    }
}
