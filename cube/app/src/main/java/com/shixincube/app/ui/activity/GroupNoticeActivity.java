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

import android.widget.ImageView;
import android.widget.TextView;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.AvatarUtils;
import com.shixincube.app.util.DateUtils;
import com.shixincube.app.util.UIUtils;

import butterknife.BindView;
import cube.contact.model.Contact;
import cube.contact.model.Group;
import cube.engine.CubeEngine;

/**
 * 群组通知界面。
 */
public class GroupNoticeActivity extends BaseActivity {

    @BindView(R.id.ivAvatar)
    ImageView avatarImage;

    @BindView(R.id.tvName)
    TextView nameText;
    @BindView(R.id.tvDate)
    TextView dateText;

    @BindView(R.id.tvNotice)
    TextView noticeText;

    public GroupNoticeActivity() {
        super();
    }

    @Override
    public void initView() {
        this.setToolbarTitle(UIUtils.getString(R.string.group_notice));
    }

    @Override
    public void initData() {
        Group group = CubeEngine.getInstance().getContactService().getGroup(getIntent().getLongExtra("groupId", 0L));
        if (null == group) {
            return;
        }

        Contact contact = group.getAppendix().getNoticeOperator();
        AvatarUtils.fillContactAvatar(this, contact, this.avatarImage);

        this.nameText.setText(contact.getPriorityName());
        this.dateText.setText(DateUtils.formatYMDHM(group.getAppendix().getNoticeTime()));
        this.noticeText.setText(group.getAppendix().getNotice());
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_group_notice;
    }
}
