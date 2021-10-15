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

package com.shixincube.app.manager;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.shixincube.app.CubeApp;

import cube.engine.CubeBinder;
import cube.engine.CubeEngine;
import cube.engine.handler.EngineHandler;

/**
 * 魔方服务连接器。
 */
public class CubeServiceConnection implements ServiceConnection {

    private Context context;

    public CubeServiceConnection(Context context) {
        this.context = context;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        if (null == CubeApp.engine) {
            CubeApp.engine = ((CubeBinder) iBinder).getEngine(this.context);

            if (null != CubeApp.engine.getConfig()) {
                // 已读取配置
                CubeApp.engine.start(new EngineHandler() {
                    @Override
                    public void handleSuccess(CubeEngine engine) {

                    }

                    @Override
                    public void handleFailure(int code, String description) {

                    }
                });
            }
            else {
                // 没有读取到配置，从服务器获取配置
                // TODO 从 App 服务器读取配置
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        // Nothing
    }
}
