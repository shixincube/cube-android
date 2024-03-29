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

package com.shixincube.app.ui.fragment;

import android.widget.LinearLayout;
import android.widget.TextView;

import com.shixincube.app.R;
import com.shixincube.app.ui.activity.BoxActivity;
import com.shixincube.app.ui.activity.MainActivity;
import com.shixincube.app.ui.activity.PreferenceActivity;
import com.shixincube.app.ui.activity.ProfileInfoActivity;
import com.shixincube.app.ui.base.BaseFragment;
import com.shixincube.app.ui.presenter.ProfilePresenter;
import com.shixincube.app.ui.view.ProfileView;
import com.shixincube.app.widget.AdvancedImageView;
import com.shixincube.app.widget.optionitemview.OptionItemView;

import butterknife.BindView;

/**
 * 个人与应用信息管理。
 */
public class ProfileFragment extends BaseFragment<ProfileView, ProfilePresenter> implements ProfileView {

    @BindView(R.id.llProfileInfo)
    LinearLayout profileInfoLayout;

    @BindView(R.id.ivAvatar)
    AdvancedImageView avatarImage;
    @BindView(R.id.tvNickname)
    TextView nickNameText;
    @BindView(R.id.tvCubeId)
    TextView cubeIdText;

    @BindView(R.id.oivSetting)
    OptionItemView settingItemView;
    @BindView(R.id.oivBox)
    OptionItemView boxItemView;
    @BindView(R.id.oivHelp)
    OptionItemView helpItemView;

    public ProfileFragment() {
        super();
    }

    @Override
    public void initListener() {
        this.profileInfoLayout.setOnClickListener((view) -> {
            ((MainActivity) getActivity()).jumpToActivityAndClearTop(ProfileInfoActivity.class);
        });

        this.settingItemView.setOnClickListener((view) -> {
            ((MainActivity) getActivity()).jumpToActivityAndClearTop(PreferenceActivity.class);
        });

        this.boxItemView.setOnClickListener((view) -> {
            ((MainActivity) getActivity()).jumpToActivityAndClearTop(BoxActivity.class);
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        this.presenter.loadAccountInfo();
    }

    @Override
    protected ProfilePresenter createPresenter() {
        return new ProfilePresenter((MainActivity) getActivity());
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.fragment_profile;
    }

    @Override
    public AdvancedImageView getAvatarImage() {
        return this.avatarImage;
    }

    @Override
    public TextView getNickNameText() {
        return this.nickNameText;
    }

    @Override
    public TextView getCubeIdText() {
        return this.cubeIdText;
    }
}
