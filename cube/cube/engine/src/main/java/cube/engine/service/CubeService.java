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

package cube.engine.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

import cube.core.KernelConfig;
import cube.engine.CubeEngine;
import cube.engine.handler.EngineHandler;
import cube.util.LogUtils;

/**
 * 自动化配置魔方服务。
 */
public class CubeService extends Service {

    public final static String ACTION_START = "start";

    public final static String ACTION_STOP = "stop";

    public final static String ACTION_RESET = "reset";

    private CubeBinder binder;

    private boolean startCompatibility;

    private AtomicBoolean startPrepare = new AtomicBoolean(false);
    private AtomicBoolean startFinish = new AtomicBoolean(false);
    private Failure startFailure = null;

    private final Object mutex = new Object();

    public CubeService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        LogUtils.d("CubeService", "onCreate");

        this.startCompatibility = getApplicationInfo().targetSdkVersion < Build.VERSION_CODES.ECLAIR;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
//        LogUtils.d("CubeService", "onStartCommand : " + flags + " | " + startId);

        if (null != intent) {
            String action = intent.getAction();
            if (null != action) {
                if (action.equals(CubeService.ACTION_START)) {
                    (new Thread() {
                        @Override
                        public void run() {
                            if (!CubeEngine.getInstance().hasStarted()) {
                                // 检查是否进行了配置
                                if (null == CubeEngine.getInstance().getConfig()) {
                                    CubeEngine.getInstance().setConfig(readConfig());
                                }

                                startPrepare.set(true);

                                CubeEngine.getInstance().start(getApplicationContext(), new EngineHandler() {
                                    @Override
                                    public void handleSuccess(CubeEngine engine) {
                                        synchronized (mutex) {
                                            startFinish.set(true);
                                            if (null != binder && !binder.handleCalled.get()
                                                    && null != binder.engineHandler) {
                                                binder.handleCalled.set(true);
                                                binder.engineHandler.handleSuccess(CubeEngine.getInstance());
                                            }
                                        }

                                        startPrepare.set(false);
                                    }

                                    @Override
                                    public void handleFailure(int code, String description) {
                                        synchronized (mutex) {
                                            startFinish.set(true);
                                            startFailure = new Failure(code, description);

                                            if (null != binder && !binder.handleCalled.get()
                                                    && null != binder.engineHandler) {
                                                binder.handleCalled.set(true);
                                                binder.engineHandler.handleFailure(code, description);
                                            }
                                        }

                                        startPrepare.set(false);
                                    }
                                });
                            }
                            else {
                                synchronized (mutex) {
                                    startFinish.set(true);
                                    if (null != binder && !binder.handleCalled.get()) {
                                        binder.handleCalled.set(true);
                                        binder.engineHandler.handleSuccess(CubeEngine.getInstance());
                                    }
                                }
                            }
                        }
                    }).start();
                }
                else if (action.equals(CubeService.ACTION_STOP)) {
                    synchronized (mutex) {
                        startPrepare.set(false);
                        startFinish.set(false);
                        startFailure = null;
                    }

                    (new Thread() {
                        @Override
                        public void run() {
                            CubeEngine.getInstance().stop();
                        }
                    }).start();
                }
                else if (action.equals(CubeService.ACTION_RESET)) {
                    synchronized (mutex) {
                        startPrepare.set(false);
                        startFinish.set(false);
                        startFailure = null;
                    }

                    (new Thread() {
                        @Override
                        public void run() {
                            // 停止引擎
                            CubeEngine.getInstance().stop();

                            // 读取配置
                            CubeEngine.getInstance().setConfig(readConfig());

                            startPrepare.set(true);

                            CubeEngine.getInstance().start(getApplicationContext(), new EngineHandler() {
                                @Override
                                public void handleSuccess(CubeEngine engine) {
                                    synchronized (mutex) {
                                        startFinish.set(true);
                                        if (null != binder && !binder.handleCalled.get()
                                                && null != binder.engineHandler) {
                                            binder.handleCalled.set(true);
                                            binder.engineHandler.handleSuccess(CubeEngine.getInstance());
                                        }
                                    }

                                    startPrepare.set(false);
                                }

                                @Override
                                public void handleFailure(int code, String description) {
                                    synchronized (mutex) {
                                        startFinish.set(true);
                                        startFailure = new Failure(code, description);

                                        if (null != binder && !binder.handleCalled.get()
                                                && null != binder.engineHandler) {
                                            binder.handleCalled.set(true);
                                            binder.engineHandler.handleFailure(code, description);
                                        }
                                    }

                                    startPrepare.set(false);
                                }
                            });
                        }
                    }).start();
                }
            }
        }

        return this.startCompatibility ? START_STICKY_COMPATIBILITY : START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.i("CubeService", "#onDestroy");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (null == this.binder) {
            this.binder = new CubeBinder(this);
        }

        return this.binder;
    }

    protected void tryFireEngineHandler() {
        (new Thread() {
            @Override
            public void run() {
                if (CubeEngine.getInstance().hasStarted()) {
                    startFinish.set(true);
                }

                synchronized (mutex) {
                    if (startFinish.get()) {
                        if (!binder.handleCalled.get() && null != binder.engineHandler) {
                            binder.handleCalled.set(true);

                            if (null == startFailure) {
                                binder.engineHandler.handleSuccess(CubeEngine.getInstance());
                            } else {
                                binder.engineHandler.handleFailure(startFailure.code, startFailure.description);
                            }
                        }
                    }
                }
            }
        }).start();
    }

    private KernelConfig readConfig() {
        Context context = getApplicationContext();
        return CubeEngine.getInstance().loadConfig(context);
    }

    private class Failure {

        public int code;

        public String description;

        public Failure(int code, String description) {
            this.code = code;
            this.description = description;
        }
    }
}
