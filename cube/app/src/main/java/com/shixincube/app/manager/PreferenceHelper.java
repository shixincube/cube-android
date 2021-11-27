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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

import com.shixincube.app.AppConsts;
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

    private int nightMode;
    private ThemeMode darkThemeMode;

    private PreferenceHelper() {
        this.nightMode = Configuration.UI_MODE_NIGHT_UNDEFINED;
        this.darkThemeMode = ThemeMode.FollowSystem;
    }

    public final static PreferenceHelper getInstance(Context context) {
        if (null == PreferenceHelper.instance.sp) {
            PreferenceHelper.instance.sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            PreferenceHelper.instance.editor = PreferenceHelper.instance.sp.edit();
            PreferenceHelper.instance.load();
        }

        return PreferenceHelper.instance;
    }

    public final static PreferenceHelper getInstance() {
        return PreferenceHelper.instance;
    }

    private void load() {
        int modeCode = this.sp.getInt(AppConsts.APP_DARK_THEME_MODE, ThemeMode.FollowSystem.code);
        this.darkThemeMode = ThemeMode.parse(modeCode);
    }

    /**
     * 设置当前系统的夜间模式。
     *
     * @param nightMode
     */
    public void setNightMode(int nightMode) {
        this.nightMode = nightMode;
    }

    /**
     * 获取夜间主题模式。
     *
     * @return
     */
    public ThemeMode getDarkThemeMode() {
        return this.darkThemeMode;
    }

    /**
     * 检查当前时间段是否应启用夜间模式。
     *
     * @return
     */
    public boolean checkEnableDarkTheme() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        // 00:00 到 07:00 夜间
        // 22:00 到 23:59 夜间

        if (hour < 7) {
            return true;
        }
        else if (hour >= 22) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * 设置深色主题模式。
     *
     * @param themeMode 指定主题模式。
     */
    public void setDarkThemeMode(ThemeMode themeMode) {
        if (this.darkThemeMode == themeMode) {
            return;
        }

        this.darkThemeMode = themeMode;
        this.editor.putInt(AppConsts.APP_DARK_THEME_MODE, themeMode.code);
        this.editor.commit();

        switch (themeMode) {
            case AlwaysOn:
                CubeBaseApp.eachActivity(new CubeBaseApp.ActivityHandler() {
                    @Override
                    public void handle(BaseActivity activity) {
                        activity.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    }
                });
                break;

            case AlwaysOff:
                CubeBaseApp.eachActivity(new CubeBaseApp.ActivityHandler() {
                    @Override
                    public void handle(BaseActivity activity) {
                        activity.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    }
                });
                break;

            default:
                switch (this.nightMode) {
                    case Configuration.UI_MODE_NIGHT_YES:
                        CubeBaseApp.eachActivity(new CubeBaseApp.ActivityHandler() {
                            @Override
                            public void handle(BaseActivity activity) {
                                activity.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            }
                        });
                        break;
                    case Configuration.UI_MODE_NIGHT_NO:
                    case Configuration.UI_MODE_NIGHT_UNDEFINED:
                        CubeBaseApp.eachActivity(new CubeBaseApp.ActivityHandler() {
                            @Override
                            public void handle(BaseActivity activity) {
                                activity.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            }
                        });
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    /**
     * 配置 Activity 的主题。
     *
     * @param activity
     */
    public void setupTheme(BaseActivity activity) {
        // MODE_NIGHT_NO, MODE_NIGHT_YES
        int localNightMode = activity.getDelegate().getLocalNightMode();

        switch (this.darkThemeMode) {
            case AlwaysOn:
                if (localNightMode != AppCompatDelegate.MODE_NIGHT_YES) {
                    activity.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
                break;
            case AlwaysOff:
                if (localNightMode != AppCompatDelegate.MODE_NIGHT_NO) {
                    activity.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                break;
            default:
                break;
        }
    }
}
