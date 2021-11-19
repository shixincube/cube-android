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

import static android.content.Context.BIND_AUTO_CREATE;

import android.content.Intent;

import com.shixincube.app.CubeApp;
import com.shixincube.app.manager.CubeConnection;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.MainView;

import cube.engine.CubeEngine;
import cube.engine.CubeService;
import cube.util.LogUtils;

/**
 * 主界面。
 */
public class MainPresenter extends BasePresenter<MainView> {

    public MainPresenter(BaseActivity activity) {
        super(activity);

        this.check();
    }

    /**
     * 检查魔方引擎服务是否启动，如果没有启动则启动
     */
    private void check() {
        if (CubeEngine.getInstance().hasSignIn()) {
            // 已调用过签入
            return;
        }

        LogUtils.i("MainPresenter", "Launch Cube Engine");

        CubeApp.getMainThreadHandler().post(() -> {
            // 使用连接服务的方式签入账号
            CubeConnection connection = new CubeConnection();
            Intent intent = new Intent(activity, CubeService.class);
            activity.bindService(intent, connection, BIND_AUTO_CREATE);
        });
    }
}
