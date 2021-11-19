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

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.shixincube.app.R;
import com.shixincube.app.manager.AccountHelper;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.OperateGroupMemberView;
import com.shixincube.app.widget.adapter.AdapterForRecyclerView;
import com.shixincube.app.widget.adapter.ViewHolderForRecyclerView;

import java.util.List;

import cube.contact.model.Contact;

/**
 * 创建群组。
 */
public class OperateGroupMemberPresenter extends BasePresenter<OperateGroupMemberView> {

    private List<Contact> selectedMembers;

    private AdapterForRecyclerView<Contact> selectedAdapter;

    public OperateGroupMemberPresenter(BaseActivity activity, List<Contact> selectedMembers) {
        super(activity);
        this.selectedMembers = selectedMembers;
    }

    public void load() {
        this.loadData();
        this.setAdapter();
    }

    private void loadData() {

    }

    private void setAdapter() {
        if (null == this.selectedAdapter) {
            this.selectedAdapter = new AdapterForRecyclerView<Contact>(activity, selectedMembers, R.layout.item_member_selected) {
                @Override
                public void convert(ViewHolderForRecyclerView helper, Contact item, int position) {
                    Glide.with(activity)
                            .load(AccountHelper.getAvatarResource(item))
                            .centerCrop()
                            .into((ImageView) helper.getView(R.id.aivAvatar));
                }
            };
            getView().getSelectedContactsView().setAdapter(this.selectedAdapter);
        }
    }
}
