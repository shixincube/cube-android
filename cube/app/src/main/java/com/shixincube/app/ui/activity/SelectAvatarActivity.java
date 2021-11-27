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
import android.widget.ImageButton;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.UIUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 选择内置头像。
 */
public class SelectAvatarActivity extends BaseActivity {

    private List<ImageButton> avatarImageList;

    private ImageButton selectedImage;

    public SelectAvatarActivity() {
        super();
        this.avatarImageList = new ArrayList<>();
    }

    @Override
    public void initView() {
        this.setToolbarTitle(UIUtils.getString(R.string.select_avatar));
    }

    @Override
    public void initListener() {
        for (int i = 1; i <= 16; ++i) {
            String stringId = String.format("ibAvatar%02d", i);
            int resId = getResources().getIdentifier(stringId, "id", getPackageName());
            ImageButton imageButton = findViewById(resId);
            this.avatarImageList.add(imageButton);

            imageButton.setOnClickListener(this::onAvatarItemClick);
        }
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_select_avatar;
    }

    private void onAvatarItemClick(View view) {
        if (selectedImage == view) {
            return;
        }

        if (null != this.selectedImage) {
            this.selectedImage.setBackgroundColor(UIUtils.getColor(R.color.transparent));
        }

        this.selectedImage = (ImageButton) view;
        this.selectedImage.setBackgroundColor(UIUtils.getColor(R.color.theme_blue_light));
    }
}
