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

import android.widget.LinearLayout;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.presenter.GroupListPresenter;
import com.shixincube.app.ui.view.GroupListView;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.recyclerview.RecyclerView;

import butterknife.BindView;

/**
 * 群组列表。
 */
public class GroupListActivity extends BaseActivity<GroupListView, GroupListPresenter> implements GroupListView {

    @BindView(R.id.llGroups)
    LinearLayout groupsLayout;

    @BindView(R.id.rvGroups)
    RecyclerView groupsView;

    @BindView(R.id.llNoGroups)
    LinearLayout noGroupsLayout;

    public GroupListActivity() {
        super();
    }

    @Override
    public void initView() {
        setToolbarTitle(UIUtils.getString(R.string.group));
    }

    @Override
    public void initData() {
        this.presenter.load();
    }

    @Override
    protected GroupListPresenter createPresenter() {
        return new GroupListPresenter(this);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_group_list;
    }

    @Override
    public LinearLayout getGroupsLayout() {
        return this.groupsLayout;
    }

    @Override
    public RecyclerView getGroupsView() {
        return this.groupsView;
    }

    @Override
    public LinearLayout getNoGroupsLayout() {
        return this.noGroupsLayout;
    }
}
