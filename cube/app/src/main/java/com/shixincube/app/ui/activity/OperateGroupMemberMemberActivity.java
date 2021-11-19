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
import com.shixincube.app.ui.presenter.OperateGroupMemberPresenter;
import com.shixincube.app.ui.view.OperateGroupMemberView;
import com.shixincube.app.widget.recyclerview.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import cube.contact.model.Contact;

/**
 * 创建群组。
 */
public class OperateGroupMemberMemberActivity extends BaseActivity<OperateGroupMemberView, OperateGroupMemberPresenter> implements OperateGroupMemberView {

    @BindView(R.id.rvSelectedContacts)
    RecyclerView selectedContactsView;

    private List<Contact> selectedMembers;

    public OperateGroupMemberMemberActivity() {
        super();
        this.selectedMembers = new ArrayList<>();
    }

    @Override
    public void init() {
        long[] memberIdList = getIntent().getLongArrayExtra("memberIdList");

    }

    @Override
    public void initData() {
        this.presenter.load();
    }

    @Override
    protected OperateGroupMemberPresenter createPresenter() {
        return new OperateGroupMemberPresenter(this, this.selectedMembers);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_operate_group_member;
    }

    @Override
    public RecyclerView getSelectedContactsView() {
        return this.selectedContactsView;
    }
}
