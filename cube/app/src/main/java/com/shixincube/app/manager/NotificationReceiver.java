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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.shixincube.app.CubeBaseApp;
import com.shixincube.app.ui.activity.MainActivity;
import com.shixincube.app.ui.activity.MessagePanelActivity;
import com.shixincube.app.ui.base.BaseActivity;

import cube.engine.misc.NotificationConfig;

/**
 * 通知接收器。
 */
public class NotificationReceiver extends BroadcastReceiver {

    private MainActivity mainActivity = null;

    public NotificationReceiver() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.mainActivity = null;

        CubeBaseApp.eachActivity(new CubeBaseApp.ActivityHandler() {
            @Override
            public void handle(BaseActivity activity) {
                if (activity instanceof MainActivity) {
                    mainActivity = (MainActivity) activity;
                }
            }
        });

        if (null != mainActivity) {
            // 直接跳转到消息面板
            Intent panelIntent = new Intent(mainActivity, MessagePanelActivity.class);
            panelIntent.putExtra(NotificationConfig.EXTRA_BUNDLE,
                    intent.getBundleExtra(NotificationConfig.EXTRA_BUNDLE));
            mainActivity.startActivity(panelIntent);
        }
        else {
            Intent launchIntent = context.getPackageManager()
                    .getLaunchIntentForPackage(context.getPackageName());
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            // 参数设置
            launchIntent.putExtra(NotificationConfig.EXTRA_BUNDLE,
                    intent.getBundleExtra(NotificationConfig.EXTRA_BUNDLE));
            context.startActivity(launchIntent);
        }
    }
}
