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

package com.shixincube.app.ui.base;

import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.appbar.AppBarLayout;
import com.jaeger.library.StatusBarUtil;
import com.shixincube.app.CubeApp;
import com.shixincube.app.R;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.CustomDialog;

import butterknife.BindView;
import butterknife.ButterKnife;

public abstract class BaseFragmentActivity<V, T extends BaseFragmentPresenter<V>> extends FragmentActivity {

    protected T presenter;
    private CustomDialog dialogForWaiting;

    @BindView(R.id.appBar)
    protected AppBarLayout appBar;

    @BindView(R.id.ivToolbarNavigation)
    protected ImageView toolbarNavigation;

    @BindView(R.id.vToolbarDivision)
    protected View toolbarDivision;

    @BindView(R.id.tvToolbarTitle)
    protected TextView toolbarTitle;

    @BindView(R.id.tvToolbarSubTitle)
    protected TextView toolbarSubTitle;

    @BindView(R.id.ibToolbarMore)
    protected ImageButton toolbarMore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CubeApp.addActivity(this);

        init();

        this.presenter = createPresenter();
        if (null != this.presenter) {
            this.presenter.attachView((V) this);
        }

        setContentView(provideContentViewId());
        ButterKnife.bind(this);

        setupAppBarAndToolbar();

        // 沉浸式状态栏
        StatusBarUtil.setColor(this, UIUtils.getColor(R.color.colorPrimaryDark), 10);

        initView();
        initData();
        initListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CubeApp.removeActivity(this);

        if (null != this.presenter) {
            this.presenter.detachView();
        }
    }

    public void setToolbarTitle(String title) {
        toolbarTitle.setText(title);
    }

    public Dialog showWaitingDialog(String tip) {
        hideWaitingDialog();

        View view = View.inflate(this, R.layout.dialog_waiting, null);
        if (!TextUtils.isEmpty(tip)) {
            ((TextView) view.findViewById(R.id.tvTip)).setText(tip);
        }

        this.dialogForWaiting = new CustomDialog(this, view, R.style.CustomDialog);
        this.dialogForWaiting.show();
        this.dialogForWaiting.setCancelable(false);

        return this.dialogForWaiting;
    }

    public void hideWaitingDialog() {
        if (null != this.dialogForWaiting) {
            this.dialogForWaiting.dismiss();
            this.dialogForWaiting = null;
        }
    }

    /**
     * 设置 AppBar 和 Toolbar
     */
    private void setupAppBarAndToolbar() {
        if (null != appBar && Build.VERSION.SDK_INT > 21) {
            appBar.setElevation(10.6f);
        }

        toolbarNavigation.setVisibility(isToolbarCanBack() ? View.VISIBLE : View.GONE);
        toolbarDivision.setVisibility(isToolbarCanBack() ? View.VISIBLE : View.GONE);

        toolbarNavigation.setOnClickListener(v -> onBackPressed());

        toolbarTitle.setPadding(isToolbarCanBack() ? 0 : 40, 0, 0, 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void init() {
        // For subclass override
    }

    public void initView() {
        // For subclass override
    }

    public void initData() {
        // For subclass override
    }

    public void initListener() {
        // For subclass override
    }

    /**
     * 用于创建 Presenter
     * @return
     */
    protected abstract T createPresenter();

    /**
     * 提供当前界面的布局文件 ID
     * @return
     */
    protected abstract int provideContentViewId();

    /**
     * 是否让 Toolbar 有返回按钮。
     * 默认可以，一般一个应用中除了主界面，其他界面都是可以有返回按钮。
     * @return
     */
    protected boolean isToolbarCanBack() {
        return true;
    }

    public void jumpToActivity(Intent intent) {
        startActivity(intent);
    }

    public BaseFragmentActivity jumpToActivity(Class<?> activity) {
        Intent intent = new Intent(this, activity);
        startActivity(intent);
        return this;
    }

    public void jumpToActivityAndClearTask(Class<?> activity) {
        Intent intent = new Intent(this, activity);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void jumpToActivityAndClearTop(Class<?> activity) {
        Intent intent = new Intent(this, activity);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
