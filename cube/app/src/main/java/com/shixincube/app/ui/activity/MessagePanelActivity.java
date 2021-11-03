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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseFragmentActivity;
import com.shixincube.app.ui.presenter.MessagePanelPresenter;
import com.shixincube.app.ui.view.MessagePanelView;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.recyclerview.RecyclerView;

import java.io.IOException;

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

    @BindView(R.id.etContent)
    EditText inputContentView;

    @BindView(R.id.btnSend)
    Button sendButton;

    @BindView(R.id.ivEmoji)
    ImageView emojiImageView;

    @BindView(R.id.ivMore)
    ImageView moreImageView;

    private Conversation conversation;

    public MessagePanelActivity() {
        super();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != this.presenter) {
            try {
                this.presenter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

        this.toolbarMore.setImageResource(R.mipmap.ic_contact_info_gray);
        this.toolbarMore.setVisibility(View.VISIBLE);
    }

    @Override
    public void initData() {
        this.presenter.markAllRead();
        this.presenter.loadMessages();
    }

    @Override
    public void initListener() {
        // 输入框事件
        inputContentView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                // Nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
                if (inputContentView.getText().toString().trim().length() > 0) {
                    sendButton.setVisibility(View.VISIBLE);
                    moreImageView.setVisibility(View.GONE);
                    // 发送正在输入消息提示
                    CubeEngine.getInstance().getMessagingService().sendTypingStatus(conversation);
                }
                else {
                    sendButton.setVisibility(View.GONE);
                    moreImageView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Nothing
            }
        });
        inputContentView.setOnFocusChangeListener((view, focus) -> {
            if (focus) {
                UIUtils.postTaskDelay(() -> messageListView.smoothMoveToPosition(messageListView.getAdapter().getItemCount() - 1), 200);
            }
        });

        // 发送按钮事件
        sendButton.setOnClickListener((view) -> {
            presenter.sendTextMessage();
        });
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

    @Override
    public EditText getInputContentView() {
        return this.inputContentView;
    }
}
