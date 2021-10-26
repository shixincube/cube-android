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

import android.content.Intent;

import com.shixincube.app.CubeApp;
import com.shixincube.app.manager.AccountHelper;
import com.shixincube.app.model.Account;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.MainView;

import cube.contact.ContactService;
import cube.contact.handler.SignHandler;
import cube.contact.model.Self;
import cube.core.ModuleError;
import cube.engine.CubeEngine;
import cube.engine.CubeService;
import cube.util.LogUtils;

/**
 * 主界面。
 */
public class MainPresenter extends BasePresenter<MainView> {

    public MainPresenter(BaseActivity activity) {
        super(activity);

        this.checkAndSignIn();
    }

    /**
     * 检查魔方引擎服务是否启动，如果没有启动则启动
     */
    private void checkAndSignIn() {
        if (CubeEngine.getInstance().hasStarted()) {
            // 已启动
            this.signIn();
            return;
        }

        LogUtils.i("MainPresenter", "Launch Cube Engine");

        CubeApp.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(activity, CubeService.class);
                intent.setAction(CubeService.ACTION_START);
                activity.startService(intent);

                signIn();
            }
        });
    }

    private void signIn() {
        (new Thread() {
            @Override
            public void run() {
                int count = 100;
                while (!CubeEngine.getInstance().isReady()) {
                    if ((--count) <= 0) {
                        break;
                    }

                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // 已启动，账号签入
                Account account = AccountHelper.getInstance().getCurrentAccount();
                boolean result = CubeEngine.getInstance().signIn(account.id, account.name, account.toJSON(), new SignHandler() {
                    @Override
                    public void handleSuccess(ContactService service, Self self) {
                        LogUtils.i("CubeApp", "SignIn success");
                    }

                    @Override
                    public void handleFailure(ContactService service, ModuleError error) {
                        LogUtils.i("CubeApp", "SignIn failure");
                    }
                });
                if (!result) {
                    LogUtils.w("CubeApp", "SignIn Error");
                }
            }
        }).start();
    }
}
