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
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.shixincube.app.R;
import com.shixincube.app.ui.activity.ContactDetailsActivity;
import com.shixincube.app.ui.activity.NewContactActivity;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.NewContactView;
import com.shixincube.app.util.AvatarUtils;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.adapter.AdapterForRecyclerView;
import com.shixincube.app.widget.adapter.ViewHolderForRecyclerView;

import java.util.ArrayList;
import java.util.List;

import cube.contact.handler.DefaultContactZoneParticipantHandler;
import cube.contact.model.ContactZone;
import cube.contact.model.ContactZoneParticipant;
import cube.contact.model.ContactZoneParticipantState;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;

/**
 * 新联系人。
 */
public class NewContactPresenter extends BasePresenter<NewContactView> {

    private ContactZone contactZone;

    private List<ContactZoneParticipant> pendingContacts;

    private AdapterForRecyclerView<ContactZoneParticipant> adapter;

    public NewContactPresenter(BaseActivity activity) {
        super(activity);
        this.pendingContacts = new ArrayList<>();
    }

    public void loadData() {
        setAdapter();

        if (null == this.contactZone) {
            this.contactZone = CubeEngine.getInstance().getContactService().getDefaultContactZone();
        }

        this.pendingContacts.clear();
        this.pendingContacts.addAll(this.contactZone.getParticipantsByExcluding(ContactZoneParticipantState.Normal));

        if (this.pendingContacts.isEmpty()) {
            getView().getNoNewContactLayout().setVisibility(View.VISIBLE);
            getView().getNewContactLayout().setVisibility(View.GONE);
        }
        else {
            getView().getNewContactLayout().setVisibility(View.VISIBLE);
            getView().getNoNewContactLayout().setVisibility(View.GONE);
        }

        this.adapter.notifyDataSetChangedWrapper();
    }

    public void agreeAddToContacts(Long participantId) {
        ContactZoneParticipant participant = this.contactZone.getParticipant(participantId);
        if (null == participant) {
            return;
        }

        activity.showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

        this.contactZone.modifyParticipant(participant, ContactZoneParticipantState.Normal,
                new DefaultContactZoneParticipantHandler(true) {
                    @Override
                    public void handleContactZoneParticipant(ContactZoneParticipant participant, ContactZone contactZone) {
                        loadData();

                        activity.hideWaitingDialog();
                    }
                }, new DefaultFailureHandler(true) {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {
                        activity.hideWaitingDialog();

                        UIUtils.showToast(UIUtils.getString(R.string.operate_failure_with_code, error.code));
                    }
                });
    }

    private void setAdapter() {
        if (null == this.adapter) {
            this.adapter = new AdapterForRecyclerView<ContactZoneParticipant>(activity, this.pendingContacts, R.layout.item_new_contact) {
                @Override
                public void convert(ViewHolderForRecyclerView helper, ContactZoneParticipant item, int position) {
                    // 头像
                    ImageView avatarImageView = helper.getView(R.id.ivAvatar);
                    Glide.with(activity).load(AvatarUtils.getAvatarResource(item.getContact())).centerCrop().into(avatarImageView);

                    // 名字
                    helper.setText(R.id.tvName, item.getContact().getPriorityName());

                    if (item.isInviter()) {
                        displayAsInviter(helper, item, position);
                    }
                    else {
                        displayAsInvitee(helper, item, position);
                    }

                    helper.getView(R.id.btnDetails).setTag(item.getId());
                    helper.getView(R.id.btnDetails).setOnClickListener(NewContactPresenter.this::onInviterDetailsClicked);
                }
            };

            getView().getNewContactsView().setAdapter(this.adapter);
        }
    }

    private void onInviterDetailsClicked(View view) {
        Long id = (Long) view.getTag();
        Intent intent = new Intent(activity, ContactDetailsActivity.class);
        intent.putExtra("zoneName", this.contactZone.getName());
        intent.putExtra("participantId", id);
        activity.startActivityForResult(intent, NewContactActivity.REQUEST_CONTACT_DETAILS);
    }

    /**
     * 以我是"邀请人"身份显示。
     *
     * @param helper
     * @param item
     * @param position
     */
    private void displayAsInviter(ViewHolderForRecyclerView helper, ContactZoneParticipant item, int position) {
        helper.setText(R.id.tvPostscript, item.getPostscript());
        helper.setViewVisibility(R.id.tvWaiting, View.VISIBLE);
        helper.setViewVisibility(R.id.btnDetails, View.GONE);
    }

    /**
     * 以我是"被邀请人"身份显示。
     *
     * @param helper
     * @param item
     * @param position
     */
    private void displayAsInvitee(ViewHolderForRecyclerView helper, ContactZoneParticipant item, int position) {
        helper.setText(R.id.tvPostscript, UIUtils.getString(R.string.you_are_the_invitee));
        helper.setViewVisibility(R.id.tvWaiting, View.GONE);
        helper.setViewVisibility(R.id.btnDetails, View.VISIBLE);
    }
}
