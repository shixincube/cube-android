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
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.presenter.ContactDetailsPresenter;
import com.shixincube.app.ui.view.ContactDetailsView;
import com.shixincube.app.util.AvatarUtils;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.optionitemview.OptionItemView;

import butterknife.BindView;
import cube.contact.model.Contact;
import cube.engine.CubeEngine;
import cube.messaging.model.Conversation;

/**
 * 联系人详情界面。
 */
public class ContactDetailsActivity extends BaseActivity<ContactDetailsView, ContactDetailsPresenter> implements ContactDetailsView {

    public final static int RESULT_NOTHING = 1000;
    public final static int RESULT_REMOVE = 2000;
    public final static int RESULT_ADD = 3000;

    @BindView(R.id.ivAvatar)
    ImageView avatarView;

    @BindView(R.id.tvName)
    TextView nameView;

    @BindView(R.id.tvCubeId)
    TextView cubeIdView;

    @BindView(R.id.tvNickName)
    TextView nickNameView;

    @BindView(R.id.oivRemarkAndTag)
    OptionItemView remarkAndTagItem;

    @BindView(R.id.tvCountriesAndRegions)
    TextView countriesAndRegionsText;

    @BindView(R.id.tvSignature)
    TextView signatureText;

    @BindView(R.id.btnChat)
    Button toChatButton;

    @BindView(R.id.btnAddToContacts)
    Button addToContactsButton;

    @BindView(R.id.rlMenu)
    RelativeLayout menuLayout;
    @BindView(R.id.svMenu)
    ScrollView menuScrollView;

    @BindView(R.id.oivAddToBlockList)
    OptionItemView addToBlockListItemView;
    @BindView(R.id.oivDelete)
    OptionItemView deleteItemView;

    private Contact contact;

    private boolean isMyContact;

    public ContactDetailsActivity() {
        super();
    }

    @Override
    public void init() {
        Intent intent = getIntent();
        Long contactId = intent.getExtras().getLong("contactId");
        this.isMyContact = intent.getBooleanExtra("isMyContact", true);
        this.contact = CubeEngine.getInstance().getContactService().getContact(contactId);
    }

    @Override
    public void initView() {
        if (null == this.contact) {
            finish();
            return;
        }

        toolbarMore.setVisibility(View.VISIBLE);

        // FIXME 暂时隐藏个性签名
        this.signatureText.setVisibility(View.GONE);

        if (this.contact.equals(CubeEngine.getInstance().getContactService().getSelf())) {
            // 我
            this.nameView.setText(this.contact.getName());
            this.nickNameView.setVisibility(View.INVISIBLE);
            this.remarkAndTagItem.setVisibility(View.GONE);
            this.countriesAndRegionsText.setVisibility(View.GONE);
            this.toChatButton.setVisibility(View.GONE);
            this.toolbarMore.setVisibility(View.GONE);
        }
        else {
            if (this.isMyContact) {
                // 我的联系人
                this.nickNameView.setVisibility(View.VISIBLE);
                this.toChatButton.setVisibility(View.VISIBLE);
                this.addToContactsButton.setVisibility(View.GONE);
            }
            else {
                // 陌生人
                this.nickNameView.setVisibility(View.INVISIBLE);
                this.toChatButton.setVisibility(View.GONE);
                this.addToContactsButton.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void initData() {
        Glide.with(this).load(AvatarUtils.getAvatarResource(this.contact)).centerCrop().into(avatarView);
        this.nameView.setText(this.contact.getPriorityName());

        this.cubeIdView.setText(UIUtils.getString(R.string.cube_id_colon, this.contact.getId().toString()));
        this.nickNameView.setText(UIUtils.getString(R.string.nickname_colon, this.contact.getName()));
    }

    @Override
    public void initListener() {
        this.toolbarMore.setOnClickListener((view) -> {
            showMenu();
        });
        this.menuLayout.setOnClickListener((view) -> {
            hideMenu();
        });

        // 删除按钮
        this.deleteItemView.setOnClickListener((view) -> {
            hideMenu();
            showMaterialDialog(UIUtils.getString(R.string.delete_from_contacts),
                    UIUtils.getString(R.string.delete_contact_content, contact.getPriorityName()),
                    UIUtils.getString(R.string.delete),
                    UIUtils.getString(R.string.cancel),
                    (v) -> {
                        hideMaterialDialog();
                        // 删除联系人
                        presenter.deleteContact();
                    },
                    (v) -> {
                        hideMaterialDialog();
                    });
        });

        this.toChatButton.setOnClickListener((view) -> {
            // 申请会话
            Conversation conversation = CubeEngine.getInstance().getMessagingService().applyConversation(contact.getId());
            if (null != conversation) {
                Intent intent = new Intent(ContactDetailsActivity.this, MessagePanelActivity.class);
                intent.putExtra("conversationId", conversation.getId());
                jumpToActivity(intent);
                finish();
            }
            else {
                UIUtils.showToast(UIUtils.getString(R.string.not_allow));
            }
        });
    }

    @Override
    protected ContactDetailsPresenter createPresenter() {
        return new ContactDetailsPresenter(this, this.contact);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_contact_details;
    }

    private void showMenu() {
        this.menuLayout.setVisibility(View.VISIBLE);
        this.deleteItemView.setVisibility(this.isMyContact ? View.VISIBLE : View.GONE);

        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 1,
                Animation.RELATIVE_TO_SELF, 0);
        animation.setDuration(200);
        this.menuScrollView.startAnimation(animation);
    }

    private void hideMenu() {
        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 1);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                menuLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        animation.setDuration(200);
        this.menuScrollView.startAnimation(animation);
    }

    @Override
    public Button getAddToContactsButton() {
        return this.addToContactsButton;
    }

    @Override
    public OptionItemView getDeleteContactItem() {
        return this.deleteItemView;
    }
}
