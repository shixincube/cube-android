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
import android.view.View;
import android.widget.Button;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.presenter.OperateContactPresenter;
import com.shixincube.app.ui.view.OperateContactView;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.recyclerview.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import cube.contact.model.Contact;
import cube.contact.model.Group;
import cube.engine.CubeEngine;

/**
 * 操作群组联系人。
 */
public class OperateContactActivity extends BaseActivity<OperateContactView, OperateContactPresenter> implements OperateContactView {

    @BindView(R.id.rvSelectedContacts)
    RecyclerView selectedContactsView;

    @BindView(R.id.rvContacts)
    RecyclerView allContactsView;

    private View headerView;

    private boolean createMode = true;
    private boolean onlyThisGroup = false;

    public Group group = null;

    private List<Contact> selectedMembers;

    public OperateContactActivity() {
        super();
        this.selectedMembers = new ArrayList<>();
    }

    @Override
    public void init() {
        long[] memberIdList = getIntent().getLongArrayExtra("memberIdList");
        if (null != memberIdList) {
            for (long id : memberIdList) {
                Contact contact = CubeEngine.getInstance().getContactService().getContact(id);
                this.selectedMembers.add(contact);
            }
        }

        long groupId = getIntent().getLongExtra("groupId", 0);
        if (groupId > 0) {
            // 有群组信息，不是创建模式
            this.createMode = false;
            this.group = CubeEngine.getInstance().getContactService().getGroup(groupId);
        }

        this.onlyThisGroup = getIntent().getBooleanExtra("onlyThisGroup", false);
    }

    @Override
    public void initView() {
        setToolbarTitle(UIUtils.getString(R.string.select_contact));

        this.toolbarFuncButton.setVisibility(View.VISIBLE);
        this.toolbarFuncButton.setText(UIUtils.getString(R.string.complete));
        this.toolbarFuncButton.setEnabled(false);

        this.headerView = View.inflate(this, R.layout.header_operate_contact, null);
    }

    @Override
    public void initData() {
        this.presenter.load(this.group);
    }

    @Override
    public void initListener() {
        this.toolbarFuncButton.setOnClickListener((view) -> {
            if (createMode) {
                // 创建群组
                presenter.createGroupAndJump();
            }
            else {
                // 管理群成员
                Intent intent = new Intent();
                List<Contact> list = presenter.getSelectedMembers();
                long[] idList = new long[list.size()];
                for (int i = 0; i < idList.length; ++i) {
                    Contact member = list.get(i);;
                    idList[i] = member.getId();
                }
                intent.putExtra("members", idList);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    @Override
    protected OperateContactPresenter createPresenter() {
        return new OperateContactPresenter(this, this.selectedMembers,
                this.createMode, this.onlyThisGroup);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_operate_contact;
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
