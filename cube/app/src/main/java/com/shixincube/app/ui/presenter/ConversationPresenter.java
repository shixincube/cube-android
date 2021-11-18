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
import android.view.ViewGroup;
import android.widget.ImageView;

import com.shixincube.app.R;
import com.shixincube.app.model.MessageConversation;
import com.shixincube.app.ui.activity.MessagePanelActivity;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.ConversationView;
import com.shixincube.app.util.DateUtils;
import com.shixincube.app.widget.adapter.AdapterForRecyclerView;
import com.shixincube.app.widget.adapter.OnItemClickListener;
import com.shixincube.app.widget.adapter.OnItemLongClickListener;
import com.shixincube.app.widget.adapter.ViewHolder;
import com.shixincube.app.widget.adapter.ViewHolderForRecyclerView;

import java.util.ArrayList;
import java.util.List;

import cube.engine.CubeEngine;
import cube.engine.util.Future;
import cube.engine.util.PromiseHandler;
import cube.engine.util.Promise;
import cube.engine.util.PromiseFuture;
import cube.messaging.MessagingRecentEventListener;
import cube.messaging.MessagingService;
import cube.messaging.model.Conversation;
import cube.messaging.model.ConversationReminded;
import cube.messaging.model.ConversationType;
import cube.util.LogUtils;

/**
 * 最近消息会话。
 */
public class ConversationPresenter extends BasePresenter<ConversationView> implements MessagingRecentEventListener {

    private List<MessageConversation> messageConversations;

    private AdapterForRecyclerView<MessageConversation> adapter;

    public ConversationPresenter(BaseActivity activity) {
        super(activity);
        this.messageConversations = new ArrayList<>();
    }

    public void loadConversations() {
        setAdapter();
        reloadData();
    }

    private void setAdapter() {
        if (null == this.adapter) {
            this.adapter = new AdapterForRecyclerView<MessageConversation>(this.activity, this.messageConversations, R.layout.item_conversation) {
                @Override
                public void convert(ViewHolderForRecyclerView helper, MessageConversation item, int position) {
                    if (item.conversation.getType() == ConversationType.Contact) {
                        ImageView avatar = helper.getView(R.id.ivAvatar);
                        avatar.setImageResource(item.avatarResourceId);

                        helper.setText(R.id.tvDisplayName, item.conversation.getContact().getPriorityName());
                        helper.setText(R.id.tvDate, DateUtils.formatConversationTime(item.conversation.getDate()));
                        helper.setText(R.id.tvContent, item.conversation.getRecentSummary());
                    }
                    else {
                        // TODO
                    }

                    // 会话是否置顶
                    if (item.conversation.focused()) {
                        helper.setBackgroundColor(R.id.flRoot, R.color.item_focus_bg);
                    }
                    else {
                        helper.setBackgroundColor(R.id.flRoot, R.color.item_bg);
                    }

                    // 会话提醒类型
                    if (item.conversation.getReminded() == ConversationReminded.Normal) {
                        if (item.conversation.getUnreadCount() > 0) {
                            helper.setText(R.id.tvBadge, Integer.toString(item.conversation.getUnreadCount()));
                            helper.setViewVisibility(R.id.tvBadge, View.VISIBLE);
                        }
                        else {
                            helper.setViewVisibility(R.id.tvBadge, View.GONE);
                        }
                    }
                    else if (item.conversation.getReminded() == ConversationReminded.Closed) {
                        if (item.conversation.getUnreadCount() > 0) {
                            helper.setViewVisibility(R.id.tvHintBadge, View.VISIBLE);
                        }
                        else {
                            helper.setViewVisibility(R.id.tvHintBadge, View.GONE);
                        }
                    }
                    else {
                        helper.setViewVisibility(R.id.tvBadge, View.GONE);
                        helper.setViewVisibility(R.id.tvHintBadge, View.GONE);
                    }
                }
            };

            // 绑定事件

            this.adapter.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(ViewHolder helper, ViewGroup parent, View itemView, int position) {
                    MessageConversation mc = messageConversations.get(position);
                    Intent intent = new Intent(activity, MessagePanelActivity.class);
                    intent.putExtra("conversationId", mc.getConversation().getId());
                    activity.jumpToActivity(intent);
                }
            });

            this.adapter.setOnItemLongClickListener(new OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(ViewHolder helper, ViewGroup parent, View itemView, int position) {
                    return false;
                }
            });

            // 设置适配器
            getView().getRecentConversationView().setAdapter(this.adapter);
        }
    }

    private void reloadData() {
        Promise.create(new PromiseHandler<List<MessageConversation>>() {
            @Override
            public void emit(PromiseFuture<List<MessageConversation>> promise) {
                MessagingService messaging = CubeEngine.getInstance().getMessagingService();

                // 从引擎获取最近会话列表
                List<Conversation> list = messaging.getRecentConversations();
                if (!list.isEmpty()) {
                    messageConversations.clear();

                    for (Conversation conversation : list) {
                        messageConversations.add(new MessageConversation(conversation));
                    }
                }
                else {
                    // TODO 没有会话，修改背景图片
                }

                promise.resolve(messageConversations);
            }
        }).thenOnMainThread(new Future<List<MessageConversation>>() {
            @Override
            public void come(List<MessageConversation> data) {
                adapter.notifyDataSetChangedWrapper();
            }
        }).catchException(new Future<Exception>() {
            @Override
            public void come(Exception throwable) {
                LogUtils.w("ConversationPresenter", throwable);
            }
        }).launch();
    }

    @Override
    public void onConversationUpdated(Conversation conversation, MessagingService service) {

    }

    @Override
    public void onConversationListUpdated(List<Conversation> conversationList, MessagingService service) {
        this.messageConversations.clear();
        for (Conversation conversation : conversationList) {
            this.messageConversations.add(new MessageConversation(conversation));
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setAdapter();
                adapter.notifyDataSetChangedWrapper();
            }
        });
    }
}
