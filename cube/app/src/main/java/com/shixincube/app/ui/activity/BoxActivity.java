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

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.presenter.BoxPresenter;
import com.shixincube.app.ui.view.BoxView;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.optionitemview.OptionItemView;

import butterknife.BindView;

/**
 * 盒子界面。
 */
public class BoxActivity extends BaseActivity<BoxView, BoxPresenter> implements BoxView {

    @BindView(R.id.oivDomainName)
    OptionItemView domainNameView;

    @BindView(R.id.oivBeginning)
    OptionItemView domainBeginningView;

    @BindView(R.id.oivEnding)
    OptionItemView domainEndingView;

    @BindView(R.id.oivLimit)
    OptionItemView domainLimitView;

    @BindView(R.id.oivInvitationCode)
    OptionItemView invitationCodeItemView;

    @BindView(R.id.oivMemberMgmt)
    OptionItemView memberManagementItemView;

    @BindView(R.id.oivStorageMgmt)
    OptionItemView storageManagementItemView;

    @BindView(R.id.oivResetBox)
    OptionItemView resetBoxItemView;

    @BindView(R.id.oivCustomerService)
    OptionItemView customerServiceItemView;

    public BoxActivity() {
        super();
    }

    @Override
    public void initView() {
        this.setToolbarTitle(UIUtils.getString(R.string.box));
    }

    @Override
    public void initListener() {
        this.getInvitationCodeItem().setOnClickListener((view) -> {
            jumpToActivity(BoxInvitationActivity.class);
        });

        this.getMemberManagementItemView().setOnClickListener((view) -> {
            jumpToActivity(BoxMemberListActivity.class);
        });
    }

    @Override
    public void initData() {
        this.presenter.loadData();
    }

    @Override
    protected BoxPresenter createPresenter() {
        return new BoxPresenter(this);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_box;
    }

    @Override
    public OptionItemView getDomainNameView() {
        return this.domainNameView;
    }

    @Override
    public OptionItemView getDomainBeginningView() {
        return this.domainBeginningView;
    }

    @Override
    public OptionItemView getDomainEndingView() {
        return this.domainEndingView;
    }

    @Override
    public OptionItemView getDomainLimitView() {
        return this.domainLimitView;
    }

    @Override
    public OptionItemView getInvitationCodeItem() {
        return this.invitationCodeItemView;
    }

    @Override
    public OptionItemView getMemberManagementItemView() {
        return this.memberManagementItemView;
    }

    @Override
    public OptionItemView getStorageManagementItemView() {
        return this.storageManagementItemView;
    }

    @Override
    public OptionItemView getResetBoxItemView() {
        return this.resetBoxItemView;
    }

    @Override
    public OptionItemView getCustomerServiceItemView() {
        return this.customerServiceItemView;
    }
}
