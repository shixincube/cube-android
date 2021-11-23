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

import com.bumptech.glide.Glide;
import com.shixincube.app.R;
import com.shixincube.app.ui.activity.ContactDetailsActivity;
import com.shixincube.app.ui.activity.ConversationDetailsActivity;
import com.shixincube.app.ui.activity.OperateGroupMemberMemberActivity;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.ConversationDetailsView;
import com.shixincube.app.util.AvatarUtils;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.AdvancedImageView;
import com.shixincube.app.widget.adapter.AdapterForRecyclerView;
import com.shixincube.app.widget.adapter.ViewHolderForRecyclerView;

import java.util.ArrayList;
import java.util.List;

import cube.contact.model.Contact;
import cube.contact.model.DummyContact;
import cube.contact.model.Group;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;
import cube.engine.util.Future;
import cube.engine.util.Promise;
import cube.engine.util.PromiseFuture;
import cube.engine.util.PromiseHandler;
import cube.messaging.handler.DefaultConversationHandler;
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

                if (conversation.getType() == ConversationType.Group) {
                    getView().getDisplayMemberNameSwitchButton().setChecked(conversation.getGroup().getAppendix().isDisplayMemberName());
                }

                adapter.notifyDataSetChangedWrapper();
            }
        }).launch();
    }

    public void clearAllMessages() {
        UIUtils.showToast(UIUtils.getString(R.string.developing));
    }

    public void quitGroup() {
        String tip = null;
        Group group = conversation.getGroup();
        if (group.isOwner()) {
            tip = UIUtils.getString(R.string.are_you_sure_to_dismiss_this_group);
        }
        else {
            tip = UIUtils.getString(R.string.you_will_never_receive_any_message_after_quit);
        }

        this.activity.showMaterialDialog(null, tip, UIUtils.getString(R.string.confirm),
            UIUtils.getString(R.string.cancel), (view) -> {
                // 隐藏对话框
                activity.hideMaterialDialog();

                activity.showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

                // 删除会话
                CubeEngine.getInstance().getMessagingService().deleteConversation(conversation, new DefaultConversationHandler(true) {
                    @Override
                    public void handleConversation(Conversation conversation) {
                        activity.hideWaitingDialog();
                        activity.setResult(ConversationDetailsActivity.RESULT_DESTROY);
                        activity.finish();
                    }
                }, new DefaultFailureHandler(true) {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {
                        activity.hideWaitingDialog();
                        UIUtils.showToast(UIUtils.getString(R.string.quit_group_fail));
                    }
                });
            }, (view) -> {
                activity.hideMaterialDialog();
            });
    }

    private void loadMembers() {
        this.members.clear();

        if (this.conversation.getType() == ConversationType.Contact) {
            this.members.add(this.conversation.getContact());
            this.members.add(new DummyContact("+"));
        }
        else if (this.conversation.getType() == ConversationType.Group) {
            this.members.addAll(this.conversation.getGroup().getMemberList());
            this.members.add(new DummyContact("+"));
            this.members.add(new DummyContact("-"));
        }
    }

    private void showContactDetails(Contact contact) {
        Intent intent = new Intent(activity, ContactDetailsActivity.class);
        intent.putExtra("contactId", contact.getId());
        intent.putExtra("allowGoToChat", false);
        activity.jumpToActivity(intent);
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
                        else if (item.getName().equals("-")) {
                            AdvancedImageView avatar = helper.getView(R.id.ivAvatar);
                            avatar.setCornerRadius(0);
                            Glide.with(activity)
                                    .load(R.mipmap.ic_group_remove_member)
                                    .centerCrop()
                                    .into(avatar);
                        }
                    }
                    else {
                        helper.setText(R.id.tvName, item.getPriorityName());
                        AdvancedImageView avatar = helper.getView(R.id.ivAvatar);
                        avatar.setCornerRadius(4);
                        Glide.with(activity)
                                .load(AvatarUtils.getAvatarResource(item))
                                .centerCrop()
                                .into(avatar);
                    }
                }
            };

            this.adapter.setOnItemClickListener((helper, parent, itemView, position) -> {
                if (conversation.getType() == ConversationType.Contact) {
                    if (position == members.size() - 1) {
                        // 创建基于群组的会话

                        // 数组长度 -1，因为数组最后一个是 "+" 视图
                        long[] selectedIdArray = new long[members.size() - 1];
                        for (int i = 0; i < selectedIdArray.length; ++i) {
                            selectedIdArray[i] = members.get(i).getId();
                        }

                        Intent intent = new Intent(activity, OperateGroupMemberMemberActivity.class);
                        intent.putExtra("memberIdList", selectedIdArray);
                        activity.startActivityForResult(intent, ConversationDetailsActivity.REQUEST_CREATE_OR_UPDATE_GROUP);
                    }
                    else {
                        // 点击的是联系人
                        Contact contact = members.get(position);
                        showContactDetails(contact);
                    }
                }
                else {
                    if (position == members.size() - 1) {
                        // 点击 "-"
                    }
                    if (position == members.size() - 2) {
                        // 点击 "+"
                    }
                    else {
                        // 点击的是联系人
                        Contact contact = members.get(position);
                        showContactDetails(contact);
                    }
                }
            });

            getView().getMemberListView().setAdapter(this.adapter);
        }
    }
}
