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

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseFragmentActivity;
import com.shixincube.app.ui.presenter.MessagePanelPresenter;
import com.shixincube.app.ui.view.MessagePanelView;
import com.shixincube.app.widget.recyclerview.RecyclerView;

import butterknife.BindView;
import cn.bingoogolapple.refreshlayout.BGARefreshLayout;
import cube.engine.CubeEngine;
import cube.messaging.model.Conversation;

/**
 * 消息面板。
 */
public class MessagePanelActivity extends BaseFragmentActivity<MessagePanelView, MessagePanelPresenter> implements MessagePanelView, BGARefreshLayout.BGARefreshLayoutDelegate {

    @BindView(R.id.rvMessages)
    RecyclerView messageListView;

    private Conversation conversation;

    public MessagePanelActivity() {
        super();
    }

    @Override
    public void init() {
        Intent intent = getIntent();
        Long conversationId = intent.getLongExtra("conversationId", 0L);
        this.conversation = CubeEngine.getInstance().getMessagingService().getConversation(conversationId);
    }

    @Override
    public void initView() {
        setToolbarTitle(this.conversation.getDisplayName());
        this.toolbarMore.setImageResource(R.mipmap.ic_contact_info_black);
        this.toolbarMore.setVisibility(View.VISIBLE);
    }

    @Override
    public void initData() {
        this.presenter.loadMessages();
    }

    @Override
    protected MessagePanelPresenter createPresenter() {
        return new MessagePanelPresenter(this, this.conversation);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_message_panel;
    }

    @Override
    public void onBGARefreshLayoutBeginRefreshing(BGARefreshLayout refreshLayout) {

    }

    @Override
    public boolean onBGARefreshLayoutBeginLoadingMore(BGARefreshLayout refreshLayout) {
        return false;
    }

    @Override
    public RecyclerView getMessageListView() {
        return this.messageListView;
    }
}
