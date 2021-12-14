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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import cube.core.KernelConfig;
import cube.engine.CubeEngine;
import cube.engine.handler.EngineHandler;

/**
 * 自动化配置魔方服务。
 */
public class CubeService extends Service {

    public final static String ACTION_START = "start";

    public final static String ACTION_STOP = "stop";

    private CubeBinder binder;

    private boolean startCompatibility;

    protected boolean startPrepare = false;
    protected boolean startFinish = false;
    protected Failure startFailure = null;

    private final Object mutex = new Object();

    public CubeService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        Log.d("CubeService", "onCreate");

        this.startCompatibility = getApplicationInfo().targetSdkVersion < Build.VERSION_CODES.ECLAIR;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
//        Log.d("CubeService", "onStartCommand : " + flags + " | " + startId);

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

                                startPrepare = true;

                                CubeEngine.getInstance().start(getApplicationContext(), new EngineHandler() {
                                    @Override
                                    public void handleSuccess(CubeEngine engine) {
                                        synchronized (mutex) {
                                            startFinish = true;
                                            if (null != binder && null != binder.engineHandler) {
                                                binder.engineHandler.handleSuccess(CubeEngine.getInstance());
                                                binder.engineHandler = null;
                                            }
                                        }
                                    }

                                    @Override
                                    public void handleFailure(int code, String description) {
                                        synchronized (mutex) {
                                            startFinish = true;
                                            startFailure = new Failure(code, description);

                                            if (null != binder && null != binder.engineHandler) {
                                                binder.engineHandler.handleFailure(code, description);
                                                binder.engineHandler = null;
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }).start();
                }
                else if (action.equals(CubeService.ACTION_STOP)) {
                    (new Thread() {
                        @Override
                        public void run() {
                            CubeEngine.getInstance().stop();
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
//        Log.d("CubeService", "onDestroy");
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
                synchronized (mutex) {
                    if (null != binder.engineHandler) {
                        if (startFinish) {
                            if (null == startFailure) {
                                binder.engineHandler.handleSuccess(CubeEngine.getInstance());
                            }
                            else {
                                binder.engineHandler.handleFailure(startFailure.code, startFailure.description);
                            }
                            binder.engineHandler = null;
                        }
                    }
                }
            }
        }).start();
    }

    private KernelConfig readConfig() {
        Context context = getApplicationContext();
        KernelConfig config = null;
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            String address = appInfo.metaData.getString("CUBE_ADDRESS");
            int port = appInfo.metaData.containsKey("CUBE_PORT") ? appInfo.metaData.getInt("CUBE_PORT") : 7000;
            String domain = appInfo.metaData.getString("CUBE_DOMAIN");
            String appKey = appInfo.metaData.getString("CUBE_APPKEY");

            if (null != address && null != domain && null != appKey) {
                config = new KernelConfig(address, port, domain, appKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return config;
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