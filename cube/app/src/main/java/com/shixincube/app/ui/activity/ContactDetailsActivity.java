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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.shixincube.app.R;
import com.shixincube.app.manager.AccountHelper;
import com.shixincube.app.model.Account;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.optionitemview.OptionItemView;

import butterknife.BindView;
import cube.contact.model.Contact;
import cube.engine.CubeEngine;

/**
 * 联系人详情界面。
 */
public class ContactDetailsActivity extends BaseActivity {

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

    private Contact contact;

    public ContactDetailsActivity() {
        super();
    }

    @Override
    public void init() {
        Intent intent = getIntent();
        Long contactId = intent.getExtras().getLong("contactId");
        this.contact = CubeEngine.getInstance().getContactService().getContact(contactId);
    }

    @Override
    public void initView() {
        if (null == this.contact) {
            finish();
            return;
        }

        toolbarMore.setVisibility(View.VISIBLE);
    }

    @Override
    public void initData() {
        String avatarName = Account.getAvatar(this.contact.getContext());
        Glide.with(this).load(AccountHelper.explainAvatarForResource(avatarName)).centerCrop().into(avatarView);
        this.nameView.setText(this.contact.getPriorityName());

        this.cubeIdView.setText(UIUtils.getString(R.string.my_cube_id, this.contact.getId().toString()));
        this.nickNameView.setText(UIUtils.getString(R.string.nickname_colon, this.contact.getName()));

        // FIXME 暂时隐藏个性签名
        this.signatureText.setVisibility(View.GONE);

        if (this.contact.equals(CubeEngine.getInstance().getContactService().getSelf())) {
            // 我
            this.nameView.setText(this.contact.getName());
            this.nickNameView.setVisibility(View.INVISIBLE);
            this.remarkAndTagItem.setVisibility(View.GONE);
            this.countriesAndRegionsText.setVisibility(View.GONE);
            this.toChatButton.setVisibility(View.GONE);
        }
        else {
            if (CubeEngine.getInstance().getContactService().getDefaultContactZone().contains(this.contact)) {
                // 我的联系人
                this.nickNameView.setVisibility(View.VISIBLE);
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
    public void initListener() {
        this.toChatButton.setOnClickListener((view) -> {
            
        });
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_contact_details;
    }
}
