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

import android.content.Intent;
import android.net.Uri;
import android.widget.Button;
import android.widget.TextView;

import com.shixincube.app.AppConsts;
import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.UIUtils;

import butterknife.BindView;

/**
 * 关于界面。
 */
public class AboutActivity extends BaseActivity {

    @BindView(R.id.tvAppName)
    TextView appNameText;

    @BindView(R.id.btnGotoWebsite)
    Button gotoWebsiteButton;

    public AboutActivity() {
        super();
    }

    @Override
    public void initView() {
        this.setToolbarTitle(UIUtils.getString(R.string.about_cube));

        this.appNameText.setText(UIUtils.getString(R.string.app_name_in_about,
                AppConsts.VERSION));
    }

    @Override
    public void initListener() {
        this.gotoWebsiteButton.setOnClickListener((view) -> {
            Intent intent = new Intent();
            intent.setData(Uri.parse("https://shixincube.com"));
            intent.setAction(Intent.ACTION_VIEW);
            startActivity(intent);
        });
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_about;
    }
}
