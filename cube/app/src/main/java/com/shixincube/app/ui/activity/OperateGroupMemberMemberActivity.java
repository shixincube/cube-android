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
import android.widget.Button;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.presenter.OperateGroupMemberPresenter;
import com.shixincube.app.ui.view.OperateGroupMemberView;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.recyclerview.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import cube.contact.model.Contact;
import cube.contact.model.Group;
import cube.engine.CubeEngine;

/**
 * 创建群组。
 */
public class OperateGroupMemberMemberActivity extends BaseActivity<OperateGroupMemberView, OperateGroupMemberPresenter> implements OperateGroupMemberView {

    @BindView(R.id.rvSelectedContacts)
    RecyclerView selectedContactsView;

    @BindView(R.id.rvContacts)
    RecyclerView allContactsView;

    private View headerView;

    private boolean createMode = true;

    public Group group = null;

    private List<Contact> selectedMembers;

    public OperateGroupMemberMemberActivity() {
        super();
        this.selectedMembers = new ArrayList<>();
    }

    @Override
    public void init() {
        long[] memberIdList = getIntent().getLongArrayExtra("memberIdList");
        for (long id : memberIdList) {
            Contact contact = CubeEngine.getInstance().getContactService().getContact(id);
            this.selectedMembers.add(contact);
        }

        long groupId = getIntent().getLongExtra("groupId", 0);
        if (groupId > 0) {
            // 有群组信息，不是创建模式
            this.createMode = false;
        }
    }

    @Override
    public void initView() {
        this.toolbarFuncButton.setVisibility(View.VISIBLE);
        this.toolbarFuncButton.setText(UIUtils.getString(R.string.complete));
        this.toolbarFuncButton.setEnabled(false);

        this.headerView = View.inflate(this, R.layout.header_group_operate, null);
    }

    @Override
    public void initData() {
        this.presenter.load();
    }

    @Override
    public void initListener() {
        this.toolbarFuncButton.setOnClickListener((view) -> {
            if (this.createMode) {
                // 创建群组
            }
            else {
                // 添加群成员
            }
        });
    }

    @Override
    protected OperateGroupMemberPresenter createPresenter() {
        return new OperateGroupMemberPresenter(this, this.selectedMembers, this.createMode);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_operate_group_member;
    }

    @Override
    public RecyclerView getSelectedContactsView() {
        return this.selectedContactsView;
    }

    @Override
    public RecyclerView getAllContactsView() {
        return this.allContactsView;
    }

    @Override
    public Button getToolbarFunctionButton() {
        return this.toolbarFuncButton;
    }

    @Override
    public View getHeaderView() {
        return this.headerView;
    }
}
