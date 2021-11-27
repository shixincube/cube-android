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
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Handler;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatDelegate;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.shixincube.app.manager.PreferenceHelper;
import com.shixincube.app.manager.ThemeMode;
import com.shixincube.app.widget.emotion.EmotionKit;
import com.shixincube.app.widget.emotion.ImageLoader;
import com.shixincube.imagepicker.ImagePicker;
import com.shixincube.imagepicker.view.CropImageView;

/**
 * 魔方应用程序入口。
 */
public class CubeApp extends CubeBaseApp {

    public CubeApp() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        setupTheme();

        EmotionKit.init(this, new ImageLoader() {
            @Override
            public void displayImage(Context context, String path, ImageView imageView) {
                Glide.with(context).load(path).centerCrop().diskCacheStrategy(DiskCacheStrategy.RESOURCE).into(imageView);
            }
        });

        Handler handler = new Handler(this.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                initImagePicker();
            }
        }, 100);
    }

    /**
     * 配置主题。
     */
    private void setupTheme() {
        int uiMode = getResources().getConfiguration().uiMode;
        int dayNightUiMode = uiMode & Configuration.UI_MODE_NIGHT_MASK;

        // 主题模式
        ThemeMode themeMode = PreferenceHelper.getInstance(this.getApplicationContext()).getDarkThemeMode();
        // 设置当前系统的夜间模式
        PreferenceHelper.getInstance().setNightMode(dayNightUiMode);

        if (themeMode == ThemeMode.FollowSystem) {
            // 跟随系统
            switch (dayNightUiMode) {
                case Configuration.UI_MODE_NIGHT_YES:
                    // 需要使用夜间主题
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                    // 不需要使用夜间主题
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    break;
                case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    // 系统未定义，进行检查
                    if (PreferenceHelper.getInstance().checkEnableDarkTheme()) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    }
                    else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    }
                    break;
                default:
                    break;
            }
        }
        else if (themeMode == ThemeMode.AlwaysOn) {
            // 启用夜间主题
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        else if (themeMode == ThemeMode.AlwaysOff) {
            // 停用夜间主题
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void initImagePicker() {
        ImagePicker picker = ImagePicker.getInstance();
        picker.setImageLoader(new com.shixincube.imagepicker.loader.ImageLoader() {
            @Override
            public void displayImage(Activity activity, String path, ImageView imageView, int width, int height) {
                Glide.with(getContext()).load(Uri.parse("file://" + path).toString()).centerCrop().into(imageView);
            }

            @Override
            public void clearMemoryCache() {
            }
        });

        // 显示拍照按钮
        picker.setShowCamera(false);
        // 允许裁剪（单选才有效）
        picker.setCrop(true);
        // 是否按矩形区域保存
        picker.setSaveRectangle(true);
        // 选中数量限制
        picker.setSelectLimit(12);
        // 裁剪框的形状
        picker.setStyle(CropImageView.Style.RECTANGLE);
        // 裁剪框的宽度。单位像素（圆形自动取宽高最小值）
        picker.setFocusWidth(800);
        // 裁剪框的高度。单位像素（圆形自动取宽高最小值）
        picker.setFocusHeight(800);
        // 保存文件的宽度。单位像素
        picker.setOutPutX(1000);
        // 保存文件的高度。单位像素
        picker.setOutPutY(1000);
    }
}
