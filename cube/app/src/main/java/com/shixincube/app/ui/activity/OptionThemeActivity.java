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

import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import com.kyleduo.switchbutton.SwitchButton;
import com.shixincube.app.R;
import com.shixincube.app.manager.PreferenceHelper;
import com.shixincube.app.manager.ThemeMode;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.UIUtils;

import butterknife.BindView;

/**
 * 主题模式设置。
 */
public class OptionThemeActivity extends BaseActivity {

    @BindView(R.id.sbFollowSystem)
    SwitchButton followSystemSwitch;

    @BindView(R.id.llManualSelection)
    LinearLayout manualSelectionLayout;

    @BindView(R.id.llDarkThemeOff)
    LinearLayout darkThemeOffLayout;
    @BindView(R.id.cbDarkThemeOff)
    CheckBox darkThemeOffCheckBox;

    @BindView(R.id.llDarkThemeOn)
    LinearLayout darkThemeOnLayout;
    @BindView(R.id.cbDarkThemeOn)
    CheckBox darkThemeOnCheckBox;

    public OptionThemeActivity() {
        super();
    }

    @Override
    public void initView() {
        setToolbarTitle(UIUtils.getString(R.string.dark_theme));
    }

    @Override
    public void initData() {
        ThemeMode darkThemeMode = PreferenceHelper.getInstance().getDarkThemeMode();

        if (darkThemeMode == ThemeMode.FollowSystem) {
            this.followSystemSwitch.setChecked(true);
            this.manualSelectionLayout.setVisibility(View.GONE);
        }
        else {
            this.followSystemSwitch.setChecked(false);
            this.manualSelectionLayout.setVisibility(View.VISIBLE);

            if (darkThemeMode == ThemeMode.AlwaysOff) {
                this.darkThemeOffCheckBox.setChecked(true);
                this.darkThemeOnCheckBox.setChecked(false);
            }
            else if (darkThemeMode == ThemeMode.AlwaysOn) {
                this.darkThemeOffCheckBox.setChecked(false);
                this.darkThemeOnCheckBox.setChecked(true);
            }
        }
    }

    @Override
    public void initListener() {
        this.followSystemSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                ThemeMode darkThemeMode = PreferenceHelper.getInstance().getDarkThemeMode();
                if (checked) {
                    if (darkThemeMode == ThemeMode.FollowSystem) {
                        return;
                    }

                    manualSelectionLayout.setVisibility(View.GONE);
                    PreferenceHelper.getInstance().setDarkThemeMode(ThemeMode.FollowSystem);
                }
                else {
                    if (darkThemeMode != ThemeMode.FollowSystem) {
                        return;
                    }

                    manualSelectionLayout.setVisibility(View.VISIBLE);
                    darkThemeOffCheckBox.setChecked(true);
                    darkThemeOnCheckBox.setChecked(false);
                    PreferenceHelper.getInstance().setDarkThemeMode(ThemeMode.AlwaysOff);
                }
            }
        });

        this.darkThemeOffLayout.setOnClickListener((view) -> {
            darkThemeOffCheckBox.setChecked(true);
            darkThemeOnCheckBox.setChecked(false);
            PreferenceHelper.getInstance().setDarkThemeMode(ThemeMode.AlwaysOff);
        });

        this.darkThemeOnLayout.setOnClickListener((view) -> {
            darkThemeOnCheckBox.setChecked(true);
            darkThemeOffCheckBox.setChecked(false);
            PreferenceHelper.getInstance().setDarkThemeMode(ThemeMode.AlwaysOn);
        });
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_option_theme;
    }
}
