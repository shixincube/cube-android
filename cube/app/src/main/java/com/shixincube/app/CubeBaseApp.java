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

package com.shixincube.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.multidex.MultiDexApplication;

import com.shixincube.app.ui.base.BaseActivity;

import java.util.LinkedList;
import java.util.List;

import cube.engine.CubeEngine;

/**
 * 基础 App 描述。
 */
public class CubeBaseApp extends MultiDexApplication {

    private static List<BaseActivity> activities = new LinkedList<>();

    // 上下文
    private static Context context;
    // 主线程
    private static Thread mainThread;
    // 主线程 ID
    private static long mainThreadId;
    // 循环队列
    private static Looper mainLooper;
    // 主线程 Handler
    private static Handler mainHandler;

    public CubeBaseApp() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // 监听应用退到后台
        CubeEngine.getInstance().monitorBackground(this);

        CubeBaseApp.context = getApplicationContext();
        CubeBaseApp.mainThread = Thread.currentThread();
        CubeBaseApp.mainThreadId = android.os.Process.myTid();
        CubeBaseApp.mainLooper = Looper.getMainLooper();
        CubeBaseApp.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 程序完全退出。
     */
    public static void exit() {
        for (Activity activity : CubeBaseApp.activities) {
            activity.finish();
        }
    }

    /**
     * 重启当前应用。
     */
    public static void restart() {
        Intent intent = CubeBaseApp.context.getPackageManager().getLaunchIntentForPackage(CubeBaseApp.context.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        CubeBaseApp.context.startActivity(intent);
    }

    public static void addActivity(BaseActivity activity) {
        if (!CubeBaseApp.activities.contains(activity)) {
            CubeBaseApp.activities.add(activity);
        }
    }

    public static void removeActivity(BaseActivity activity) {
        CubeBaseApp.activities.remove(activity);
    }

    public static void eachActivity(ActivityHandler handler) {
        for (BaseActivity activity : CubeBaseApp.activities) {
            handler.handle(activity);
        }
    }

    public static Context getContext() {
        return CubeBaseApp.context;
    }

    public static void setContext(Context context) {
        CubeBaseApp.context = context;
    }

    public static Thread getMainThread() {
        return CubeBaseApp.mainThread;
    }

    public static long getMainThreadId() {
        return CubeBaseApp.mainThreadId;
    }

    public static Handler getMainThreadHandler() {
        return CubeBaseApp.mainHandler;
    }


    /**
     * Activity 句柄。
     */
    public interface ActivityHandler {
        void handle(BaseActivity activity);
    }
}
