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

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;

/**
 * Popup Window 实用函数库。
 */
public final class PopupWindowUtils {

    private PopupWindowUtils() {
    }

    /**
     * 最核心的 PopupWindow 创建方法。
     *
     * @param contentView PopupWindow 要显示的视图
     * @param width       PopupWindow 的宽度
     * @param height      PopupWindow 的高度
     * @return
     */
    @NonNull
    private static PopupWindow getPopupWindow(View contentView, int width, int height) {
        PopupWindow popupWindow = new PopupWindow(contentView, width, height, true);
        popupWindow.setOutsideTouchable(false);
        openOutsideTouchable(popupWindow);
        return popupWindow;
    }

    /**
     * 点击 PopupWindow 范围以外的地方时隐藏。
     *
     * @param popupWindow
     */
    public static void openOutsideTouchable(PopupWindow popupWindow) {
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        popupWindow.setOutsideTouchable(true);
    }

    /**
     * 得到指定在某个视图内位置的 PopupWindow 并显示。
     *
     * @param contentView PopupWindow 要显示的视图
     * @param width       PopupWindow 的宽度
     * @param height      PopupWindow 的高度
     * @param parentView  参考视图
     * @param gravityType 在参考视图中的相对位置
     * @param xOff        X 轴偏移量
     * @param yOff        Y 轴偏移量
     * @return
     */
    public static PopupWindow getPopupWindowAtLocation(View contentView, int width, int height, View parentView, int gravityType, int xOff, int yOff) {
        PopupWindow popupWindow = getPopupWindow(contentView, width, height);

        // 在 ParentView 中偏移 xOff 和 yOff
        popupWindow.showAtLocation(parentView,
                gravityType, xOff, yOff);

        return popupWindow;
    }

    /**
     * 使 Window 变亮。
     *
     * @param activity
     */
    public static void makeWindowLight(Activity activity) {
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.alpha = 1f;
        activity.getWindow().setAttributes(lp);
    }

    /**
     * 使 Window 变暗。
     *
     * @param activity
     */
    public static void makeWindowDark(Activity activity) {
        makeWindowDark(activity, 0.7f);
    }

    private static void makeWindowDark(Activity activity, float alpha) {
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.alpha = alpha;
        activity.getWindow().setAttributes(lp);
    }
}