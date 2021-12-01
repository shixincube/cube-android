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

package com.shixincube.app.widget.keyboard;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * 软键盘工具。
 */
public class SoftwareKeyboard implements View.OnTouchListener, ViewTreeObserver.OnGlobalLayoutListener {

    private static final String SHARE_PREFERENCE_NAME = "CubeSoftwareKeyboard";
    private static final String SHARE_PREFERENCE_SOFT_INPUT_HEIGHT = "soft_input_height";

    private Activity activity;

    private int windowHeight;

    private int softInputHeight;

    // 软键盘管理类
    private InputMethodManager inputManager;

    private SharedPreferences sharedPreferences;

    // 内容布局view,即除了表情布局或者软键盘布局以外的布局，用于固定bar的高度，防止跳闪
    private View contentView;

    private EditText hostEditText;

    // 表情布局
    private List<Stuff> stuffList;

    protected ButtonOnClickListener buttonOnClickListener;

    /**
     * 构造函数。
     */
    public SoftwareKeyboard() {
        this.windowHeight = 0;
        this.softInputHeight = 0;
        this.stuffList = new ArrayList<>();
    }

    public static SoftwareKeyboard with(Activity activity) {
        SoftwareKeyboard softwareInputDetector = new SoftwareKeyboard();
        softwareInputDetector.activity = activity;
        softwareInputDetector.inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        softwareInputDetector.sharedPreferences = activity.getSharedPreferences(SHARE_PREFERENCE_NAME, Context.MODE_PRIVATE);
        activity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(softwareInputDetector);
        softwareInputDetector.softInputHeight = softwareInputDetector.dip2px(300);
        return softwareInputDetector;
    }

    public void destroy() {
        this.hostEditText.setOnTouchListener(null);
        this.activity.getWindow().getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    public void close() {
        this.contentView.setVisibility(View.GONE);
        for (Stuff stuff : this.stuffList) {
            stuff.layout.setVisibility(View.GONE);
        }
    }

    /**
     * 绑定内容view，此view用于固定bar的高度，防止跳闪
     */
    public SoftwareKeyboard bindToContent(View contentView) {
        this.contentView = contentView;
        return this;
    }

    /**
     * 绑定编辑框
     */
    public SoftwareKeyboard bindToEditText(EditText editText) {
        this.hostEditText = editText;
        this.hostEditText.requestFocus();
        this.hostEditText.setOnTouchListener(this);
        return this;
    }

    /**
     * 绑定表情按钮（可以有多个表情按钮）
     *
     * @param layout
     * @param button
     * @return
     */
    public SoftwareKeyboard bindToLayoutAndButton(View layout, View button) {
        this.stuffList.add(new Stuff(layout, button));
        button.setOnClickListener(getButtonOnClickListener());
        return this;
    }

    public void setButtonOnClickListener(ButtonOnClickListener buttonOnClickListener) {
        this.buttonOnClickListener = buttonOnClickListener;
    }

    private View.OnClickListener getButtonOnClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (buttonOnClickListener != null) {
                    if (buttonOnClickListener.onButtonClickListener(view)) {
                        return;
                    }
                }

                View layout = getShownLayout();

                if (null != layout) {
                    // 显示软键盘时，锁定内容高度，防止跳闪。
                    lockContentHeight();

                    // 隐藏布局，显示软键盘
                    hideLayout(layout, false);

                    // 判读是否显示新布局
                    View current = findLayoutWithButton(view);
                    if (null != current && layout != current) {
                        showLayout(current);

//                        if (!contentView.isShown()) {
//                            contentView.setVisibility(View.VISIBLE);
//                        }
                        showView(contentView);
                    }
                    else {
//                        if (contentView.isShown()) {
//                            contentView.setVisibility(View.GONE);
//                        }
                        hideView(contentView);
                    }

                    // 软键盘显示后，释放内容高度
                    unlockContentHeightDelayed();
                }
                else {
                    if (isSoftInputShown()) {
                        // 显示软键盘时，锁定内容高度，防止跳闪。
                        lockContentHeight();

                        View current = findLayoutWithButton(view);
                        if (null != current) {
                            showLayout(current);
                        }
                        else {
                            showLayout(stuffList.get(0).layout);
                        }

                        if (!contentView.isShown()) {
                            contentView.getLayoutParams().height = softInputHeight;
//                            contentView.setVisibility(View.VISIBLE);
                            showView(contentView);
                        }

                        unlockContentHeightDelayed();
                    }
                    else {
                        View current = findLayoutWithButton(view);
                        if (null != current) {
                            showLayout(current);
                        }
                        else {
                            showLayout(stuffList.get(0).layout);
                        }

//                        if (!contentView.isShown()) {
//                            contentView.setVisibility(View.VISIBLE);
//                        }
                        showView(contentView);
                    }
                }
            }
        };
    }

    private void showView(View view) {
        if (!view.isShown()) {
            view.setVisibility(View.VISIBLE);
            TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 1,
                    Animation.RELATIVE_TO_SELF, 0);
            animation.setDuration(200);
            view.startAnimation(animation);
        }
    }

    private void hideView(View view) {
        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 1);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        animation.setDuration(200);
        view.startAnimation(animation);
    }

    private View getShownLayout() {
        for (Stuff stuff : this.stuffList) {
            if (stuff.layout.isShown()) {
                return stuff.layout;
            }
        }

        return null;
    }

    private View findLayoutWithButton(View button) {
        for (Stuff stuff : this.stuffList) {
            if (button == stuff.button || button.getId() == stuff.button.getId()) {
                return stuff.layout;
            }
        }

        return null;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        View layout = getShownLayout();
        boolean shown = (null != layout && layout.isShown());

        if (motionEvent.getAction() == MotionEvent.ACTION_UP && shown) {
            // 显示软键盘时，锁定内容高度，防止跳闪。
            lockContentHeight();

            // 隐藏布局，显示软键盘
            hideLayout(layout, true);

            if (contentView.isShown()) {
                contentView.setVisibility(View.GONE);
            }

            // 软键盘显示后，释放内容高度
            hostEditText.postDelayed(new Runnable() {
                @Override
                public void run() {
                    unlockContentHeightDelayed();
                }
            }, 200L);
        }

        return false;
    }

    @Override
    public void onGlobalLayout() {
        Rect r = new Rect();
        //获取当前窗口实际的可见区域
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
        int height = r.height();
        if (windowHeight == 0) {
            // 一般情况下，这是原始的窗口高度
            windowHeight = height;
        }
        else {
            if (windowHeight != height) {
                // 两次窗口高度相减就是软键盘高度
                if (this.softInputHeight == 0) {
                    this.softInputHeight = windowHeight - height;
                    sharedPreferences.edit().putInt(SHARE_PREFERENCE_SOFT_INPUT_HEIGHT, this.softInputHeight).apply();
                }
            }
        }
    }

    public SoftwareKeyboard build() {
        // 设置软键盘的模式：SOFT_INPUT_ADJUST_RESIZE 这个属性表示 Activity 的主窗口总是会被调整大小，从而保证软键盘显示空间。
        // 从而方便我们计算软键盘的高度
        this.activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        // 隐藏软键盘
        hideSoftInput();
        return this;
    }

    /**
     * 点击返回键时先隐藏布局。
     *
     * @return
     */
    public boolean interceptBackPress() {
        View layout = this.getShownLayout();
        if (null != layout) {
            hideLayout(layout, false);

            if (this.contentView.isShown()) {
                this.contentView.setVisibility(View.GONE);
            }

            return true;
        }
        return false;
    }

    private void showLayout(View layout) {
        if (this.softInputHeight == 0) {
            int max = dip2px(300);
            this.softInputHeight = this.sharedPreferences.getInt(SHARE_PREFERENCE_SOFT_INPUT_HEIGHT, max);
            if (this.softInputHeight > max) {
                this.softInputHeight = max;
                this.sharedPreferences.edit().putInt(SHARE_PREFERENCE_SOFT_INPUT_HEIGHT, this.softInputHeight).apply();
            }
        }
        hideSoftInput();
        layout.getLayoutParams().height = this.softInputHeight;
        layout.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏表情布局
     *
     * @param showSoftInput 是否显示软键盘
     */
    private void hideLayout(View layout, boolean showSoftInput) {
        if (layout.isShown()) {
            layout.setVisibility(View.GONE);
            if (showSoftInput) {
                showSoftInput();
            }
        }
    }

    private int dip2px(int dip) {
        float density = this.activity.getApplicationContext().getResources().getDisplayMetrics().density;
        int px = (int) (dip * density + 0.5f);
        return px;
    }

    /**
     * 锁定内容高度，防止跳闪
     */
    private void lockContentHeight() {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) this.contentView.getLayoutParams();
        params.height = this.contentView.getHeight();
        params.weight = 0.0f;
    }

    /**
     * 释放被锁定的内容高度
     */
    public void unlockContentHeightDelayed() {
        this.hostEditText.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((LinearLayout.LayoutParams) contentView.getLayoutParams()).weight = 1.0f;
            }
        }, 200L);
    }

    /**
     * 编辑框获取焦点，并显示软键盘
     */
    private void showSoftInput() {
        this.hostEditText.requestFocus();
        this.hostEditText.post(new Runnable() {
            @Override
            public void run() {
                inputManager.showSoftInput(hostEditText, 0);
            }
        });
    }

    /**
     * 隐藏软键盘
     */
    private void hideSoftInput() {
        this.inputManager.hideSoftInputFromWindow(this.hostEditText.getWindowToken(), 0);
    }

    /**
     * 是否显示软键盘
     *
     * @return
     */
    public boolean isSoftInputShown() {
        int screenHeight = activity.getWindow().getDecorView().getHeight();
        Rect rect = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        int diff = Math.abs(screenHeight - rect.bottom - getSoftButtonsBarHeight());
        int delta = (int) Math.floor(screenHeight * 0.1f);
        return diff > delta;
    }

    /*private int getSupportSoftInputHeight() {
        Rect r = new Rect();
        // decorView 是 Window 中的最顶层 view，可以从 Window 中通过 getDecorView 获取到 decorView 。
        // 通过 decorView 获取到程序显示的区域，包括标题栏，但不包括状态栏。
        this.activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
        // 获取屏幕的高度
        int screenHeight = this.activity.getWindow().getDecorView().getRootView().getHeight();
        // 计算软键盘的高度
        int softInputHeight = screenHeight - r.bottom;

        // 某些 Android 版本下，没有显示软键盘时减出来的高度总是144，而不是零，
        // 这是因为高度是包括了虚拟按键栏的(例如华为系列)，所以在API Level高于20时，
        // 我们需要减去底部虚拟按键栏的高度（如果有的话）
        if (Build.VERSION.SDK_INT >= 20) {
            // When SDK Level >= 20 (Android L), the softInputHeight will contain the height of softButtonsBar (if has)
            softInputHeight = softInputHeight - getSoftButtonsBarHeight();
        }
        if (softInputHeight < 0) {
            Log.w(this.getClass().getSimpleName(), "Warning: value of softInputHeight is below zero!");
        }
        // 存一份到本地
        if (softInputHeight > 0) {
            sharedPreferences.edit().putInt(SHARE_PREFERENCE_SOFT_INPUT_HEIGHT, softInputHeight).apply();
        }
        return softInputHeight;
    }*/

    /**
     * 底部虚拟按键栏的高度
     *
     * @return
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private int getSoftButtonsBarHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        // 这个方法获取可能不是真实屏幕的高度
        this.activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int usableHeight = metrics.heightPixels;
        // 获取当前屏幕的真实高度
        this.activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        int realHeight = metrics.heightPixels;
        if (realHeight > usableHeight) {
            return realHeight - usableHeight;
        }
        else {
            return 0;
        }
    }

//    public int getKeyBoardHeight() {
//        return sharedPreferences.getInt(SHARE_PREFERENCE_SOFT_INPUT_HEIGHT, 180);
//    }

    public interface ButtonOnClickListener {

        boolean onButtonClickListener(View view);
    }

    protected class Stuff {

        protected final View layout;

        protected final View button;

        protected Stuff(View layout, View button) {
            this.layout = layout;
            this.button = button;
        }
    }
}
