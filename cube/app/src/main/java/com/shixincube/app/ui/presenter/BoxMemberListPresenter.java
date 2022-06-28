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

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.shixincube.app.R;
import com.shixincube.app.ui.activity.BoxMemberDetailsActivity;
import com.shixincube.app.ui.activity.BoxMemberListActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.AvatarUtils;
import com.shixincube.app.widget.adapter.AdapterForRecyclerView;
import com.shixincube.app.widget.adapter.OnItemClickListener;
import com.shixincube.app.widget.adapter.ViewHolder;
import com.shixincube.app.widget.adapter.ViewHolderForRecyclerView;
import com.shixincube.app.widget.recyclerview.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cube.auth.model.AuthDomain;
import cube.contact.model.Contact;
import cube.contact.model.Self;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;
import cube.ferry.handler.DomainHandler;
import cube.ferry.model.DomainInfo;
import cube.ferry.model.DomainMember;
import cube.ferry.model.Role;

/**
 * 讯盒成员列表。
 */
public class BoxMemberListPresenter extends BasePresenter {

    private List<DomainMember> domainMemberList;

    private AdapterForRecyclerView<DomainMember> adapter;

    private RecyclerView listView;

    private Role myRole;

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

        Self self = CubeEngine.getInstance().getContactService().getSelf();

        CubeEngine.getInstance().getFerryService().getDomain(new DomainHandler() {
            @Override
            public void handleDomain(AuthDomain authDomain, DomainInfo domainInfo, List<DomainMember> members) {
                domainMemberList.clear();
                domainMemberList.addAll(members);

                for (DomainMember member : members) {
                    if (member.getContactId().equals(self.id)) {
                        myRole = member.getRole();
                        break;
                    }
                }

                adapter.notifyDataSetChangedWrapper();
            }
        }, new DefaultFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {

            }
        });
    }

    /**
     * 列表适配器。
     */
    protected class ListAdapter extends AdapterForRecyclerView<DomainMember> implements OnItemClickListener {

        public ListAdapter() {
            super(activity, domainMemberList, R.layout.item_domain_member);

            // 设置点击事件
            setOnItemClickListener(this);
        }

        @Override
        public void convert(ViewHolderForRecyclerView helper, DomainMember item, int position) {
            Contact contact = CubeEngine.getInstance().getContactService().getContact(item.getContactId());
            // 名称
            helper.setText(R.id.tvName, contact.getPriorityName());

            // 头像
            AvatarUtils.fillContactAvatar(activity, contact, (ImageView) helper.getView(R.id.ivAvatar));

            if (item.getRole() == Role.Administrator) {
                helper.setViewVisibility(R.id.ivRoleAdmin, View.VISIBLE);
            }
            else {
                helper.setViewVisibility(R.id.ivRoleAdmin, View.GONE);
            }
        }

        @Override
        public void onItemClick(ViewHolder helper, ViewGroup parent, View itemView, int position) {
            DomainMember member = domainMemberList.get(position);
            Intent intent = new Intent(activity, BoxMemberDetailsActivity.class);
            intent.putExtra(BoxMemberDetailsActivity.EXTRA_DOMAIN_MEMBER, member);
            intent.putExtra(BoxMemberDetailsActivity.EXTRA_MY_ROLE, myRole.code);
            activity.startActivityForResult(intent, BoxMemberListActivity.REQUEST_MEMBER_DETAILS);
        }
    }
}
