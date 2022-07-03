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

import android.annotation.SuppressLint;

import com.shixincube.app.util.FileUtils;

/**
 * 应用程序常量。
 */
public final class AppConsts {

    public final static String TAG = "Cube";

    /**
     * 程序主版本。
     */
    public final static int VERSION_MAJOR = 3;

    /**
     * 程序副版本。
     */
    public final static int VERSION_MINOR = 0;

    /**
     * 程序修订号。
     */
    public final static int VERSION_REVISION = 0;

    /**
     * 应用程序版本。
     */
    @SuppressLint("DefaultLocale")
    public final static String VERSION = String.format("%d.%d.%d", VERSION_MAJOR, VERSION_MINOR, VERSION_REVISION);

    /**
     * 是否启用数据摆渡模式。
     */
    public final static boolean FERRY_MODE = true;

    /**
     * App 服务器地址。
     */
    public final static String APP_SERVER_ADDRESS = "192.168.0.108";

    /**
     * 默认国际电话区号。
     */
    public final static String REGION_CODE = "86";

    /**
     * 令牌码。
     */
    public final static String TOKEN_CODE = "AppTokenCode";

    /**
     * 令牌过期时间戳。
     */
    public final static String TOKEN_EXPIRE = "AppTokenExpire";

    /**
     * 令牌对应的账号。
     */
    public final static String APP_ACCOUNT = "AppAccount";

    /**
     * 深色主题模式。
     */
    public final static String APP_DARK_THEME_MODE = "AppDarkThemeMode";

    /**
     * 图片保存位置。
     */
    public final static String IMAGE_DIR = FileUtils.getDir("image");

    /**
     * 摄像机视频文件保存位置。
     */
    public final static String CAMERA_VIDEO_DIR = FileUtils.getDir("camera_video");

    /**
     * 摄像机照片文件保存位置。
     */
    public final static String CAMERA_PHOTO_DIR = FileUtils.getDir("camera_photo");

    /**
     * 语音输入模式配置属性属性名。
     */
    public final static String VOICE_INPUT_MODE = "voiceInputMode";

    /**
     * 阅后即焚模式配置属性属性名。
     */
    public final static String BURN_MODE = "burnMode";
}
