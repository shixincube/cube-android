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

import com.bumptech.glide.Glide;
import com.shixincube.app.R;
import com.shixincube.app.manager.AccountHelper;
import com.shixincube.app.model.Account;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.ConversationDetailsView;
import com.shixincube.app.widget.AdvancedImageView;
import com.shixincube.app.widget.adapter.AdapterForRecyclerView;
import com.shixincube.app.widget.adapter.ViewHolderForRecyclerView;

import java.util.ArrayList;
import java.util.List;

import cube.contact.model.Contact;
import cube.contact.model.DummyContact;
import cube.engine.util.Future;
import cube.engine.util.Promise;
import cube.engine.util.PromiseFuture;
import cube.engine.util.PromiseHandler;
import cube.messaging.model.Conversation;
import cube.messaging.model.ConversationReminded;
import cube.messaging.model.ConversationType;

/**
 * 会话详情。
 */
public class ConversationDetailsPresenter extends BasePresenter<ConversationDetailsView> {

    private Conversation conversation;

    private List<Contact> members;

    private AdapterForRecyclerView adapter;

    public ConversationDetailsPresenter(BaseActivity activity, Conversation conversation) {
        super(activity);
        this.conversation = conversation;
        this.members = new ArrayList<>();
    }

    public void load() {
        setAdapter();

        Promise.create(new PromiseHandler<List<Contact>>() {
            @Override
            public void emit(PromiseFuture<List<Contact>> promise) {
                loadMembers();
                promise.resolve(members);
            }
        }).thenOnMainThread(new Future<List<Contact>>() {
            @Override
            public void come(List<Contact> data) {
                getView().getCloseRemindSwitchButton().setChecked(!(conversation.getReminded() == ConversationReminded.Normal));
                getView().getTopConversationSwitchButton().setChecked(conversation.focused());

                adapter.notifyDataSetChangedWrapper();
            }
        }).launch();
    }

    private void loadMembers() {
        this.members.clear();

        if (this.conversation.getType() == ConversationType.Contact) {
            this.members.add(this.conversation.getContact());
            this.members.add(new DummyContact("+"));
        }
        else if (this.conversation.getType() == ConversationType.Group) {

        }
    }

    private void setAdapter() {
        if (null == this.adapter) {
            this.adapter = new AdapterForRecyclerView<Contact>(activity, this.members, R.layout.item_member_frame) {
                @Override
                public void convert(ViewHolderForRecyclerView helper, Contact item, int position) {
                    if (item instanceof DummyContact) {
                        helper.setText(R.id.tvName, "");

                        if (item.getName().equals("+")) {
                            AdvancedImageView avatar = helper.getView(R.id.ivAvatar);
                            avatar.setCornerRadius(0);
                            Glide.with(activity)
                                    .load(R.mipmap.ic_group_add_member)
                                    .centerCrop()
                                    .into(avatar);
                        }
                    }
                    else {
                        helper.setText(R.id.tvName, item.getPriorityName());
                        AdvancedImageView avatar = helper.getView(R.id.ivAvatar);
                        avatar.setCornerRadius(4);
                        String avatarName = Account.getAvatar(item.getContext());
                        Glide.with(activity)
                                .load(AccountHelper.explainAvatarForResource(avatarName))
                                .centerCrop()
                                .into(avatar);
                    }
                }
            };

            this.adapter.setOnItemClickListener((helper, parent, itemView, position) -> {

            });

            getView().getMemberListView().setAdapter(this.adapter);
        }
    }
}
