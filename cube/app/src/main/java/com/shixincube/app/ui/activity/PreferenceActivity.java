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

package com.shixincube.app.ui.activity;

import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.shixincube.app.AppConsts;
import com.shixincube.app.R;
import com.shixincube.app.manager.PreferenceHelper;
import com.shixincube.app.manager.ThemeMode;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.optionitemview.OptionItemView;

import butterknife.BindView;
import cube.auth.model.AuthDomain;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;
import cube.ferry.handler.DefaultDomainMemberHandler;
import cube.ferry.model.DomainInfo;
import cube.ferry.model.DomainMember;

/**
 * 偏好设置。
 */
public class PreferenceActivity extends BaseActivity {

    @BindView(R.id.oivDarkTheme)
    OptionItemView darkThemeItem;

    @BindView(R.id.oivAbout)
    OptionItemView aboutItem;

    @BindView(R.id.btnQuit)
    Button quitFerryButton;

    public PreferenceActivity() {
        super();
    }

    @Override
    public void onResume() {
        super.onResume();

        this.initData();
    }

    @Override
    public void initView() {
        setToolbarTitle(UIUtils.getString(R.string.setting));

        if (AppConsts.FERRY_MODE) {
            this.quitFerryButton.setVisibility(View.VISIBLE);
        }
        else {
            this.quitFerryButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void initData() {
        ThemeMode mode = PreferenceHelper.getInstance().getDarkThemeMode();
        switch (mode) {
            case AlwaysOn:
                this.darkThemeItem.setEndText(UIUtils.getString(R.string.dark_theme_enable));
                break;
            case AlwaysOff:
                this.darkThemeItem.setEndText(UIUtils.getString(R.string.dark_theme_disable));
                break;
            default:
                this.darkThemeItem.setEndText(UIUtils.getString(R.string.follow_system));
                break;
        }
    }

    @Override
    public void initListener() {
        this.darkThemeItem.setOnClickListener((view) -> {
            jumpToActivity(OptionThemeActivity.class);
        });

        this.aboutItem.setOnClickListener((view) -> {
            jumpToActivity(AboutActivity.class);
        });

        this.quitFerryButton.setOnClickListener((view) -> {
            quitFerryDomain();
        });
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_preference;
    }

    private void quitFerryDomain() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(UIUtils.getString(R.string.prompt));
        builder.setMessage(UIUtils.getString(R.string.ferry_do_you_want_to_quit));
        builder.setPositiveButton(UIUtils.getString(R.string.sure), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                CubeEngine.getInstance().getFerryService().quitDomain(PreferenceActivity.this,
                        new DefaultDomainMemberHandler(true) {
                    @Override
                    public void handleDomainMember(AuthDomain authDomain, DomainInfo domainInfo, DomainMember member) {
                        // 跳转回 Ferry 界面
                        jumpToActivityAndClearTask(FerryActivity.class);
                    }
                }, new DefaultFailureHandler(true) {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {
                        UIUtils.showToast(UIUtils.getString(R.string.ferry_quit_failed));
                    }
                });
            }
        });
        builder.setNegativeButton(UIUtils.getString(R.string.cancel), null);
        builder.show();
    }
}
