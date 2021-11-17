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
import android.widget.TextView;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.presenter.ConversationDetailsPresenter;
import com.shixincube.app.ui.view.ConversationDetailsView;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.recyclerview.RecyclerView;

import butterknife.BindView;
import cube.engine.CubeEngine;
import cube.messaging.model.Conversation;

/**
 * 会话详情。
 */
public class ConversationDetailsActivity extends BaseActivity<ConversationDetailsView, ConversationDetailsPresenter> implements ConversationDetailsView {

    @BindView(R.id.tvToolbarTitle)
    TextView toolbarTitleTextView;

    private Conversation conversation;

    public ConversationDetailsActivity() {
        super();
    }

    @Override
    public void init() {
        Intent intent = getIntent();
        Long conversationId = intent.getLongExtra("conversationId", 0);
        this.conversation = CubeEngine.getInstance().getMessagingService().getConversation(conversationId);
    }

    @Override
    public void initView() {
        this.toolbarTitleTextView.setText(UIUtils.getString(R.string.conv_title));
    }

    @Override
    protected ConversationDetailsPresenter createPresenter() {
        return new ConversationDetailsPresenter(this, this.conversation);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_conversation_details;
    }

    @Override
    public RecyclerView getMemberListView() {
        return null;
    }
}
