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
import android.widget.ImageView;

import com.shixincube.app.R;
import com.shixincube.app.ui.activity.MessagePanelActivity;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.GroupListView;
import com.shixincube.app.util.AvatarUtils;
import com.shixincube.app.widget.adapter.AdapterForRecyclerView;
import com.shixincube.app.widget.adapter.ViewHolderForRecyclerView;

import java.util.ArrayList;
import java.util.List;

import cube.contact.model.ContactZone;
import cube.contact.model.ContactZoneParticipant;
import cube.engine.CubeEngine;

/**
 * 群组列表。
 */
public class GroupListPresenter extends BasePresenter<GroupListView> {

    private List<ContactZoneParticipant> groupList;

    private ContactZone zone;

    private AdapterForRecyclerView<ContactZoneParticipant> adapter;

    public GroupListPresenter(BaseActivity activity) {
        super(activity);
        this.groupList = new ArrayList<>();
    }

    public void load() {
        if (null == this.zone) {
            this.zone = CubeEngine.getInstance().getContactService().getDefaultGroupZone();
        }

        this.groupList.clear();
        this.groupList.addAll(this.zone.getOrderedParticipants());

        setAdapter();
    }

    private void setAdapter() {
        if (null == this.adapter) {
            this.adapter = new AdapterForRecyclerView<ContactZoneParticipant>(activity, this.groupList, R.layout.item_group) {
                @Override
                public void convert(ViewHolderForRecyclerView helper, ContactZoneParticipant item, int position) {
                    ImageView avatarImageView = helper.getView(R.id.ivAvatar);
                    AvatarUtils.fillGroupAvatar(activity, item.getGroup(), avatarImageView);

                    helper.setText(R.id.tvName, item.getGroup().getPriorityName());
                    helper.setText(R.id.tvMembersNum, Integer.toString(item.getGroup().numMembers()));
                }
            };

            this.adapter.setOnItemClickListener((helper, parent, itemView, position) -> {
                Intent intent = new Intent(activity, MessagePanelActivity.class);
                intent.putExtra("conversationId", this.groupList.get(position).getId());
                activity.jumpToActivity(intent);
                activity.finish();
            });

            getView().getGroupsView().setAdapter(this.adapter);
        }
        else {
            this.adapter.notifyDataSetChangedWrapper();
        }
    }
}
