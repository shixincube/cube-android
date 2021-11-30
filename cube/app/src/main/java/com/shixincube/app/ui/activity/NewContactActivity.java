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
import android.widget.LinearLayout;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.presenter.NewContactPresenter;
import com.shixincube.app.ui.view.NewContactView;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.recyclerview.RecyclerView;

import butterknife.BindView;

/**
 * 新联系人。
 */
public class NewContactActivity extends BaseActivity<NewContactView, NewContactPresenter> implements NewContactView {

    public final static int REQUEST_CONTACT_DETAILS = 1001;

    @BindView(R.id.llNoNewContact)
    LinearLayout noNewContactLayout;

    @BindView(R.id.llNewContact)
    LinearLayout newContactLayout;

    @BindView(R.id.rvNewContacts)
    RecyclerView newContactsView;

    public NewContactActivity() {
        super();
    }

    @Override
    public void initView() {
        this.setToolbarTitle(UIUtils.getString(R.string.new_contact));
    }

    @Override
    public void initData() {
        this.presenter.loadData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CONTACT_DETAILS) {
            if (resultCode == ContactDetailsActivity.RESULT_PENDING_AGREE) {
                Long participantId = data.getLongExtra("participantId", 0L);
                presenter.agreeAddToContacts(participantId);
            }
        }
    }

    @Override
    protected NewContactPresenter createPresenter() {
        return new NewContactPresenter(this);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_new_contact;
    }

    @Override
    public LinearLayout getNoNewContactLayout() {
        return this.noNewContactLayout;
    }

    @Override
    public LinearLayout getNewContactLayout() {
        return this.newContactLayout;
    }

    @Override
    public RecyclerView getNewContactsView() {
        return this.newContactsView;
    }
}
