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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.AvatarUtils;
import com.shixincube.app.util.UIUtils;

import butterknife.BindView;
import cube.contact.model.Contact;
import cube.engine.CubeEngine;
import cube.ferry.model.DomainMember;
import cube.ferry.model.Role;

/**
 * 讯盒成员详情。
 */
public class BoxMemberDetailsActivity extends BaseActivity {

    public final static String EXTRA_DOMAIN_MEMBER = "domainMember";
    public final static String EXTRA_MY_ROLE = "myRole";

    @BindView(R.id.ivAvatar)
    ImageView avatarView;

    @BindView(R.id.tvName)
    TextView nameView;

    @BindView(R.id.tvCubeId)
    TextView cubeIdView;

    @BindView(R.id.tvNickName)
    TextView nickNameView;

    @BindView(R.id.tvSignature)
    TextView signatureText;

    @BindView(R.id.llRole)
    LinearLayout roleItemLayout;
    @BindView(R.id.tvRole)
    TextView roleTextView;

    @BindView(R.id.btnChat)
    Button toChatButton;

    @BindView(R.id.btnAddToContacts)
    Button addToContactsButton;

    @BindView(R.id.llMemberOption)
    LinearLayout memberOptionLayout;

    private DomainMember domainMember;
    private Contact contact;

    private Role myRole;

    public BoxMemberDetailsActivity() {
        super();
    }

    @Override
    public void init() {
        Intent intent = getIntent();
        this.domainMember = intent.getParcelableExtra(EXTRA_DOMAIN_MEMBER);
        this.myRole = Role.parse(intent.getIntExtra(EXTRA_MY_ROLE, Role.Member.code));
        this.contact = CubeEngine.getInstance().getContactService().getContact(this.domainMember.getContactId());
    }

    @Override
    public void initView() {
        // FIXME 暂时隐藏个性签名
        this.signatureText.setVisibility(View.GONE);

        this.roleItemLayout.setVisibility(View.VISIBLE);

        this.toChatButton.setVisibility(View.GONE);
        this.addToContactsButton.setVisibility(View.GONE);
    }

    @Override
    public void initData() {
        Glide.with(this).load(AvatarUtils.getAvatarResource(this.contact)).centerCrop().into(avatarView);
        this.nameView.setText(this.contact.getPriorityName());

        this.nickNameView.setText(UIUtils.getString(R.string.nickname_colon, this.contact.getName()));
        this.cubeIdView.setText(UIUtils.getString(R.string.cube_id_colon, this.contact.getId().toString()));

        if (Role.Administrator == this.domainMember.getRole()) {
            this.roleTextView.setText(UIUtils.getString(R.string.member_role_admin));
        }
        else {
            this.roleTextView.setText(UIUtils.getString(R.string.member_role_member));
        }

        if (this.myRole == Role.Administrator) {
            memberOptionLayout.setVisibility(View.VISIBLE);
        }
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
