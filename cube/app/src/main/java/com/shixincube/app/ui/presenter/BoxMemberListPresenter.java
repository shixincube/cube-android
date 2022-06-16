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

package com.shixincube.app.ui.presenter;

import com.shixincube.app.R;
import com.shixincube.app.ui.activity.BoxMemberListActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.widget.adapter.AdapterForRecyclerView;
import com.shixincube.app.widget.adapter.ViewHolderForRecyclerView;
import com.shixincube.app.widget.recyclerview.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cube.ferry.model.DomainMember;

/**
 * 讯盒成员列表。
 */
public class BoxMemberListPresenter extends BasePresenter {

    private List<DomainMember> domainMemberList;

    private AdapterForRecyclerView<DomainMember> adapter;

    private RecyclerView listView;

    public BoxMemberListPresenter(BoxMemberListActivity activity) {
        super(activity);
        this.domainMemberList = new ArrayList<>();
    }

    public void load(RecyclerView listView) {
        if (null == this.adapter) {
            this.listView = listView;
            this.adapter = new ListAdapter();
            this.listView.setAdapter(this.adapter);
        }
    }

    protected class ListAdapter extends AdapterForRecyclerView<DomainMember> {

        public ListAdapter() {
            super(activity, domainMemberList, R.layout.item_domain_member);
        }

        @Override
        public void convert(ViewHolderForRecyclerView helper, DomainMember item, int position) {

        }
    }
}
