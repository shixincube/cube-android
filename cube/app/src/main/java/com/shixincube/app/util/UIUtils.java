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

package com.shixincube.app.util;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.widget.Toast;

import com.shixincube.app.CubeBaseApp;
import com.shixincube.app.R;

/**
 * UI 实用函数库。
 */
public final class UIUtils {

    private static Toast sToast;

    private UIUtils() {
    }

    /**
     * 弹出吐司提示。
     *
     * @param text 提示文本。
     */
    public static void showToast(String text) {
        showToast(text, Toast.LENGTH_SHORT);
    }

    /**
     * 弹出吐司提示。
     *
     * @param text 提示文本。
     * @param duration 持续时长模式。
     */
    public static void showToast(String text, int duration) {
        if (null == sToast) {
            sToast = Toast.makeText(getContext(), "", duration);
        }
        sToast.setText(text);
        sToast.show();
    }

    /**
     * 在其他线程中安全地弹出吐司提示。
     *
     * @param text 吐司文本。
     */
    public static void showToastSafely(final String text) {
        getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                showToast(text);
            }
        });
    }

    public static Context getContext() {
        return CubeBaseApp.getContext();
    }

    public static Handler getMainThreadHandler() {
        return CubeBaseApp.getMainThreadHandler();
    }

    /**
     * 在主线程中延迟执行任务。
     *
     * @param task
     * @param delayMillis
     */
    public static void postTaskDelay(Runnable task, int delayMillis) {
        getMainThreadHandler().postDelayed(task, delayMillis);
    }

    /**
     * 获取资源对象。
     *
     * @return
     */
    public static Resources getResources() {
        return getContext().getResources();
    }

    /**
     * 获取 colors.xml 的颜色值。
     *
     * @param colorId
     * @return
     */
    public static int getColor(int colorId) {
        return getResources().getColor(colorId, getContext().getTheme());
    }

    /**
     * 获取 strings.xml 的字符串值。
     *
     * @param stringId
     * @return
     */
    public static String getString(int stringId) {
        return getResources().getString(stringId);
    }

    /**
     * 获取 strings.xml 的字符串值。
     *
     * @param stringId
     * @param formatArgs
     * @return
     */
    public static String getString(int stringId, Object... formatArgs) {
        return getResources().getString(stringId, formatArgs);
    }

    public static String getPackageName() {
        return getContext().getPackageName();
    }

    /**
     * 获取文件类型对应的图标。
     *
     * @param fileType
     * @return
     */
    public static int getFileIcon(String fileType) {
        if (fileType.equalsIgnoreCase("jpg")
                || fileType.equalsIgnoreCase("png")
                || fileType.equalsIgnoreCase("jpeg")
                || fileType.equalsIgnoreCase("gif")
                || fileType.equalsIgnoreCase("bmp")
                || fileType.equalsIgnoreCase("webp")) {
            return R.mipmap.ic_file_image;
        }
        else if (fileType.equalsIgnoreCase("doc")
                || fileType.equalsIgnoreCase("docx")) {
            return R.mipmap.ic_file_docx;
        }
        else if (fileType.equalsIgnoreCase("ppt")
                || fileType.equalsIgnoreCase("pptx")) {
            return R.mipmap.ic_file_pptx;
        }
        else if (fileType.equalsIgnoreCase("xls")
                || fileType.equalsIgnoreCase("xlsx")) {
            return R.mipmap.ic_file_xlsx;
        }
        else if (fileType.equalsIgnoreCase("pdf")) {
            return R.mipmap.ic_file_pdf;
        }
        else if (fileType.equalsIgnoreCase("zip")
                || fileType.equalsIgnoreCase("rar")
                || fileType.equalsIgnoreCase("7z")) {
            return R.mipmap.ic_file_zip;
        }
        else if (fileType.equalsIgnoreCase("txt")
                || fileType.equalsIgnoreCase("log")) {
            return R.mipmap.ic_file_txt;
        }
        else {
            return R.mipmap.ic_file;
        }
    }

    public static int dp2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }
}
