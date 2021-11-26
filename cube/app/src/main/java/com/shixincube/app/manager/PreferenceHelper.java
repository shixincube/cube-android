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

import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.shixincube.app.CubeBaseApp;
import com.shixincube.app.ui.base.BaseActivity;

import java.util.Calendar;

/**
 * 偏好数据存取。
 */
public final class PreferenceHelper {

    private final static PreferenceHelper instance = new PreferenceHelper();

    private final static String SP_NAME = "cube.preference";

    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    private ThemeMode darkThemeMode;

    private boolean darkThemeEnabled;

    private PreferenceHelper() {
        this.darkThemeMode = ThemeMode.FollowSystem;
    }

    public final static PreferenceHelper getInstance() {
        return PreferenceHelper.instance;
    }

    public ThemeMode getDarkThemeMode() {
        return this.darkThemeMode;
    }

    public boolean checkEnableDarkTheme() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        // 00:00 到 07:00 夜间
        // 19:00 到 23:59 夜间

        if (hour < 7) {
            return true;
        }
        else if (hour >= 19) {
            return true;
        }
        else {
            return false;
        }
    }

    public void enableDarkTheme() {
        if (this.darkThemeEnabled) {
            return;
        }

        CubeBaseApp.eachActivity(new CubeBaseApp.ActivityHandler() {
            @Override
            public void handle(BaseActivity activity) {
                activity.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        });
    }

    public void disableDarkTheme() {
        if (!this.darkThemeEnabled) {
            return;
        }

        CubeBaseApp.eachActivity(new CubeBaseApp.ActivityHandler() {
            @Override
            public void handle(BaseActivity activity) {
                activity.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }
}
