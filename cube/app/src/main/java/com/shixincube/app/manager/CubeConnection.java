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

package com.shixincube.app.manager;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.os.IBinder;

import com.shixincube.app.AppConsts;
import com.shixincube.app.CubeBaseApp;
import com.shixincube.app.model.Account;

import java.io.IOException;
import java.io.InputStream;

import cube.contact.ContactService;
import cube.contact.handler.DefaultSignHandler;
import cube.contact.model.Self;
import cube.core.ModuleError;
import cube.engine.service.CubeBinder;
import cube.engine.CubeEngine;
import cube.engine.handler.EngineHandler;
import cube.engine.util.Future;
import cube.engine.util.Promise;
import cube.engine.util.PromiseFuture;
import cube.engine.util.PromiseHandler;
import cube.filestorage.FileStorage;
import cube.filestorage.handler.UploadFileHandler;
import cube.filestorage.model.FileAnchor;
import cube.filestorage.model.FileLabel;
import cube.util.LogUtils;

/**
 * 魔方服务连接器。
 */
public class CubeConnection implements ServiceConnection {

    private Runnable successHandler;

    private Runnable failureHandler;

    public CubeConnection() {
    }

    public void setSuccessHandler(Runnable handler) {
        this.successHandler = handler;
    }

    public void setFailureHandler(Runnable handler) {
        this.failureHandler = handler;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        CubeBinder cubeBinder = (CubeBinder) iBinder;
        cubeBinder.setEngineHandler(new EngineHandler() {
            @Override
            public void handleSuccess(CubeEngine engine) {
                LogUtils.i(AppConsts.TAG, "Engine start success");
                // 设置联系人数据提供器
                engine.getContactService().setContactDataProvider(AccountHelper.getInstance());

                // 启动消息模块
                engine.getMessagingService().start();

                // 启动多方通信模块
                engine.getMultipointComm().start();

                if (AppConsts.FERRY_MODE) {
                    // 启动摆渡机模块
                    engine.getFerryService().start();
                }

                // 签入账号
                signIn(() -> {
                    // 暖机
                    LogUtils.i(AppConsts.TAG, "Engine warmup");
                    long timestamp = System.currentTimeMillis();
                    CubeEngine.getInstance().warmup();
                    LogUtils.i(AppConsts.TAG, "Engine warmup elapsed: " + (System.currentTimeMillis() - timestamp) + " ms");

                    if (null != successHandler) {
                        successHandler.run();
                    }
                }, () -> {
                    // Nothing
                }, () -> {
                    if (null != failureHandler) {
                        failureHandler.run();
                    }
                });
            }

            @Override
            public void handleFailure(int code, String description) {
                LogUtils.i(AppConsts.TAG, "Engine start failure");

                if (null != failureHandler) {
                    failureHandler.run();
                }
            }
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        // Nothing
    }

    private void signIn(Runnable startedHandler, Runnable successHandler, Runnable failureHandler) {
        Promise.create(new PromiseHandler<Account>() {
            @Override
            public void emit(PromiseFuture<Account> promise) {
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
                if (null != account) {
                    // 触发
                    promise.resolve(account);
                }
                else {
                    // 没有账号
                    promise.reject();
                }
            }
        }).then(new Future<Account>() {
            @Override
            public void come(Account account) {
                boolean result = CubeEngine.getInstance().signIn(account.id, account.name, account.toJSON(), new DefaultSignHandler(true) {
                    @Override
                    public void handleSuccess(ContactService service, Self self) {
                        LogUtils.i(AppConsts.TAG, "SignIn success");
                        successHandler.run();
                    }

                    @Override
                    public void handleFailure(ContactService service, ModuleError error) {
                        LogUtils.i(AppConsts.TAG, "SignIn failure");
                        failureHandler.run();
                    }
                });

                if (!result) {
                    failureHandler.run();
                    LogUtils.w(AppConsts.TAG, "SignIn error");
                }
                else {
                    startedHandler.run();
                }
            }
        }).launch();
    }

    private void test() {
        System.out.println("#test");

        (new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                FileStorage fileStorage = CubeEngine.getInstance().getFileStorage();

                AssetManager assetManager = CubeBaseApp.getContext().getAssets();

                try {
                    InputStream is = assetManager.open("emoji/emoji.xml");
                    fileStorage.uploadFile("emoji.xml", is, new UploadFileHandler() {
                        @Override
                        public void handleStarted(FileAnchor anchor) {
                            System.out.println("XJW : handleStarted");
                        }

                        @Override
                        public void handleProcessing(FileAnchor anchor) {
                            System.out.println("XJW : handleProcessing");
                        }

                        @Override
                        public void handleSuccess(FileAnchor anchor, FileLabel fileLabel) {
                            System.out.println("XJW : handleSuccess : " + fileLabel.getFileCode() + " - " + fileLabel.getFileName());
                        }

                        @Override
                        public void handleFailure(ModuleError error, FileAnchor anchor) {
                            System.out.println("XJW : handleFailure : " + error.code);
                        }

                        @Override
                        public boolean isInMainThread() {
                            return false;
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                }
            }
        }).start();
    }
}
