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
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.AvatarUtils;
import com.shixincube.app.widget.AdvancedImageView;

import butterknife.BindView;
import cube.contact.model.Contact;
import cube.contact.model.Group;
import cube.engine.CubeEngine;

/**
 * 简单内容输入。
 */
public class SimpleContentInputActivity extends BaseActivity {

    @BindView(R.id.tvTitle)
    TextView titleText;

    @BindView(R.id.tvTip)
    TextView tipText;

    @BindView(R.id.ivAvatar)
    AdvancedImageView avatarImage;

    @BindView(R.id.etContent)
    EditText contentText;

    @BindView(R.id.btnSubmit)
    Button submitButton;

    public SimpleContentInputActivity() {
        super();
    }

    @Override
    public void initView() {
        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        if (!TextUtils.isEmpty(title)) {
            this.titleText.setText(title);
        }
        else {
            this.titleText.setVisibility(View.GONE);
        }

        String tip = intent.getStringExtra("tip");
        if (!TextUtils.isEmpty(tip)) {
            this.tipText.setText(tip);
        }
        else {
            this.tipText.setVisibility(View.GONE);
        }

        String content = intent.getStringExtra("content");
        if (!TextUtils.isEmpty(content)) {
            this.contentText.setText(content);
        }

        long groupId = intent.getLongExtra("groupId", 0);
        if (groupId != 0) {
            Group group = CubeEngine.getInstance().getContactService().getGroup(groupId);
            AvatarUtils.fillGroupAvatar(this, group, this.avatarImage);
        }
        else {
            long contactId = intent.getLongExtra("contactId", 0);
            if (contactId != 0) {
                Contact contact = CubeEngine.getInstance().getContactService().getContact(contactId);
                AvatarUtils.fillContactAvatar(this, contact, this.avatarImage);
            }
        }

        this.contentText.requestFocus();
    }

    @Override
    public void initListener() {
        this.submitButton.setOnClickListener((view) -> {
            String content = contentText.getText().toString();
            if (!TextUtils.isEmpty(content) && content.length() > 4) {
                Intent intent = new Intent();
                intent.putExtra("content", content);
                setResult(RESULT_OK, intent);
            }
            else {
                setResult(RESULT_CANCELED);
            }

            finish();
        });
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_simple_content_input;
    }
}
