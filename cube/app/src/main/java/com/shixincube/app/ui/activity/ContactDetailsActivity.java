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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.presenter.ContactDetailsPresenter;
import com.shixincube.app.ui.view.ContactDetailsView;
import com.shixincube.app.util.AvatarUtils;
import com.shixincube.app.util.DateUtils;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.optionitemview.OptionItemView;

import butterknife.BindView;
import cube.contact.handler.DefaultContactZoneHandler;
import cube.contact.model.Contact;
import cube.contact.model.ContactZone;
import cube.contact.model.ContactZoneParticipant;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;
import cube.messaging.model.Conversation;

/**
 * 联系人详情界面。
 */
public class ContactDetailsActivity extends BaseActivity<ContactDetailsView, ContactDetailsPresenter> implements ContactDetailsView {

    public final static int MODE_MY_CONTACT = 1;
    public final static int MODE_STRANGE_CONTACT = 2;
    public final static int MODE_PENDING_CONTACT = 3;

    public final static int REQUEST_POSTSCRIPT = 1001;
    public final static int REQUEST_SET_REMARK_AND_TAG = 1002;

    public final static int RESULT_REMOVE = 101;
    public final static int RESULT_ADD = 102;
    public final static int RESULT_PENDING_AGREE = 201;

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

    @BindView(R.id.llPostscript)
    LinearLayout postscriptLayout;
    @BindView(R.id.tvPostscriptDate)
    TextView postscriptDateText;
    @BindView(R.id.tvPostscript)
    TextView postscriptText;

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

    /**
     * 工作模式。
     */
    private int mode;

    private boolean readOnly;

    private Contact contact;

    private String zoneName;
    private ContactZoneParticipant participant;

    public ContactDetailsActivity() {
        super();
    }

    @Override
    public void init() {
        Intent intent = getIntent();
        Long contactId = intent.getLongExtra("contactId", 0L);
        if (contactId.longValue() > 0) {
            if (intent.getBooleanExtra("isMyContact", true)) {
                this.mode = MODE_MY_CONTACT;
            }
            else {
                this.mode = MODE_STRANGE_CONTACT;
            }

            this.contact = CubeEngine.getInstance().getContactService().getContact(contactId);

            // 更新数据
            (new Thread() {
                @Override
                public void run() {
                    CubeEngine.getInstance().getContactService().updateContactCache(contactId);
                }
            }).start();
        }
        else {
            this.mode = MODE_PENDING_CONTACT;
            this.zoneName = intent.getStringExtra("zoneName");
            Long participantId = intent.getLongExtra("participantId", 0L);
            ContactZone zone = CubeEngine.getInstance().getContactService().getContactZone(this.zoneName);
            this.participant = zone.getParticipant(participantId);
            this.contact = this.participant.getContact();
        }

        this.readOnly = intent.getBooleanExtra("readOnly", false);
    }

    @Override
    public void initView() {
        if (null == this.contact && null == this.participant) {
            finish();
            return;
        }

        // FIXME 暂时隐藏个性签名
        this.signatureText.setVisibility(View.GONE);

        this.postscriptLayout.setVisibility(View.GONE);

        if (this.contact.equals(CubeEngine.getInstance().getContactService().getSelf()) || this.readOnly) {
            // 我
            this.nameView.setText(this.contact.getName());
            this.nickNameView.setVisibility(this.readOnly ? View.VISIBLE : View.INVISIBLE);
            this.remarkAndTagItem.setVisibility(View.GONE);
            this.addToContactsButton.setVisibility(View.GONE);
            this.toChatButton.setVisibility(View.GONE);
            this.toolbarMore.setVisibility(View.GONE);
        }
        else {
            if (this.mode == MODE_MY_CONTACT) {
                // 我的联系人
                this.nickNameView.setVisibility(View.VISIBLE);
                this.toChatButton.setVisibility(View.VISIBLE);
                this.addToContactsButton.setVisibility(View.GONE);
                this.toolbarMore.setVisibility(View.VISIBLE);
            }
            else if (this.mode == MODE_STRANGE_CONTACT) {
                // 陌生人
                this.nickNameView.setVisibility(View.INVISIBLE);
                this.toChatButton.setVisibility(View.GONE);
                this.addToContactsButton.setVisibility(View.VISIBLE);
                // 不显示右上角的"更多"按钮
                this.toolbarMore.setVisibility(View.GONE);
            }
            else {
                // 待处理联系人
                this.postscriptLayout.setVisibility(View.VISIBLE);
                this.nickNameView.setVisibility(View.INVISIBLE);
                this.toChatButton.setVisibility(View.GONE);
                this.addToContactsButton.setVisibility(View.VISIBLE);
                // 不显示右上角的"更多"按钮
                this.toolbarMore.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void initData() {
        Glide.with(this).load(AvatarUtils.getAvatarResource(this.contact)).centerCrop().into(avatarView);
        this.nameView.setText(this.contact.getPriorityName());

        this.nickNameView.setText(UIUtils.getString(R.string.nickname_colon, this.contact.getName()));
        this.cubeIdView.setText(UIUtils.getString(R.string.cube_id_colon, this.contact.getId().toString()));

        if (this.mode == MODE_PENDING_CONTACT) {
            this.postscriptDateText.setText(DateUtils.formatYMDHM(this.participant.getTimestamp()));
            this.postscriptText.setText(this.participant.getPostscript());
        }
    }

    @Override
    public void initListener() {
        this.toolbarMore.setOnClickListener((view) -> {
            showMenu();
        });
        this.menuLayout.setOnClickListener((view) -> {
            hideMenu();
        });

        // 设置标签和备注
        this.remarkAndTagItem.setOnClickListener((view) -> {
            Intent intent = new Intent(this, RemarkTagActivity.class);
            intent.putExtra("contactId", this.contact.getId().longValue());
            startActivityForResult(intent, REQUEST_SET_REMARK_AND_TAG);
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
                // 转到消息面板
                Intent intent = new Intent(ContactDetailsActivity.this, MessagePanelActivity.class);
                intent.putExtra("conversationId", conversation.getId());
                jumpToActivity(intent);
                finish();
            }
            else {
                UIUtils.showToast(UIUtils.getString(R.string.not_allow));
            }
        });

        this.addToContactsButton.setOnClickListener((view) -> {
            if (MODE_PENDING_CONTACT == this.mode) {
                // 通过验证，添加为联系人
                Intent intent = new Intent();
                intent.putExtra("zoneName", zoneName);
                intent.putExtra("participantId", participant.getId().longValue());
                setResult(RESULT_PENDING_AGREE, intent);
                finish();
            }
            else {
                // 填写附言
                Intent intent = new Intent(this, PostscriptActivity.class);
                startActivityForResult(intent, REQUEST_POSTSCRIPT);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_POSTSCRIPT) {
            if (resultCode == RESULT_OK) {
                String postscript = data.getStringExtra("postscript");

                // 向默认分区添加待处理的联系人
                CubeEngine.getInstance().getContactService().getDefaultContactZone()
                        .addParticipant(contact, postscript, new DefaultContactZoneHandler(true) {
                            @Override
                            public void handleContactZone(ContactZone contactZone) {
                                // 关闭当前界面
                                Intent intent = new Intent();
                                intent.putExtra("contactId", contact.getId().longValue());
                                setResult(RESULT_ADD, intent);
                                finish();
                            }
                        }, new DefaultFailureHandler(true) {
                            @Override
                            public void handleFailure(Module module, ModuleError error) {
                                // 提示发送附言失败
                                showMaterialDialog(UIUtils.getString(R.string.add_contact),
                                        UIUtils.getString(R.string.not_allow_add_to_contacts),
                                        UIUtils.getString(R.string.sure),
                                        null,
                                        new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                hideMaterialDialog();
                                            }
                                        },
                                        null);
                            }
                        });
            }
        }
        else if (requestCode == REQUEST_SET_REMARK_AND_TAG) {
            if (resultCode == RESULT_OK) {
                // 更新
                this.nameView.setText(this.contact.getPriorityName());
                this.nickNameView.setText(UIUtils.getString(R.string.nickname_colon, this.contact.getName()));
            }
        }
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
        this.deleteItemView.setVisibility(MODE_MY_CONTACT == this.mode ? View.VISIBLE : View.GONE);

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
