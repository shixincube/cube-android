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
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.shixincube.app.R;
import com.shixincube.app.model.MessageConversation;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.AvatarUtils;
import com.shixincube.app.util.PopupWindowUtils;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.adapter.AdapterForRecyclerView;
import com.shixincube.app.widget.adapter.ViewHolder;
import com.shixincube.app.widget.adapter.ViewHolderForRecyclerView;
import com.shixincube.app.widget.recyclerview.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import cube.engine.CubeEngine;
import cube.engine.util.Future;
import cube.engine.util.Promise;
import cube.engine.util.PromiseFuture;
import cube.engine.util.PromiseHandler;
import cube.messaging.MessagingService;
import cube.messaging.model.Conversation;
import cube.messaging.model.ConversationType;
import cube.util.LogUtils;

/**
 * 选择会话界面。
 */
public class ConversationPickerActivity extends BaseActivity {

    public final static int RESULT_BACK = 101;

    public final static String EXTRA_MESSAGE_ID = "messageId";

    @BindView(R.id.rvConversations)
    RecyclerView conversationListView;

    private long messageId;

    private boolean singleMode;

    private ConversationAdapter adapter;

    private List<MessageConversation> messageConversations;

    public ConversationPickerActivity() {
        super();
        this.singleMode = true;
        this.messageConversations = new ArrayList<>();
    }

    @Override
    public void initView() {
        this.setToolbarTitle(UIUtils.getString(R.string.title_select_one_conversation));
        this.toolbarFuncButton.setText(R.string.multiple_choice);
        this.toolbarFuncButton.setVisibility(View.VISIBLE);

        this.adapter = new ConversationAdapter();
        this.conversationListView.setAdapter(this.adapter);
    }

    @Override
    public void initData() {
        this.messageId = getIntent().getLongExtra(EXTRA_MESSAGE_ID, 0);
        this.reloadData();
    }

    @Override
    public void initListener() {
        this.toolbarFuncButton.setOnClickListener((view) -> {
            switchSelectMode();
        });

        this.adapter.setOnItemClickListener((helper, parent, itemView, position) -> {
            fireItemClick(helper, itemView, position);
        });
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_conversation_picker;
    }

    @Override
    public void onBackPressed() {
        if (singleMode) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_MESSAGE_ID, this.messageId);
            setResult(RESULT_BACK, intent);
            finish();
        }
        else {
            // 退出多选模式
            switchSelectMode();
        }
    }

    private void switchSelectMode() {
        singleMode = !singleMode;

        setToolbarTitle(singleMode ? UIUtils.getString(R.string.title_select_one_conversation) :
                UIUtils.getString(R.string.title_select_multiple_conversations));

        toolbarFuncButton.setText(singleMode ? R.string.multiple_choice : R.string.complete);
        toolbarFuncButton.setEnabled(singleMode);

        adapter.notifyDataSetChangedWrapper();
    }

    private void fireItemClick(ViewHolder helper, View itemView, int position) {
        if (this.singleMode) {
            Resources resources = this.getResources();
            DisplayMetrics dm = resources.getDisplayMetrics();
            int screenWidth = dm.widthPixels;

            View windowView = View.inflate(this, R.layout.window_message_forward, null);
            PopupWindow popupWindow = PopupWindowUtils.getPopupWindowAtLocation(windowView,
                    screenWidth - 200, ViewGroup.LayoutParams.WRAP_CONTENT,
                    getWindow().getDecorView(), Gravity.CENTER, 0, 0);
            PopupWindowUtils.makeWindowDarkOnDismissLight(this, popupWindow);
            // 关闭外部触发
            PopupWindowUtils.closeOutsideTouchable(popupWindow);

            windowView.findViewById(R.id.btnCancel).setOnClickListener((view) -> {
                popupWindow.dismiss();
            });
            windowView.findViewById(R.id.btnSend).setOnClickListener((view) -> {
                popupWindow.dismiss();
            });
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
                    synchronized (messageConversations) {
                        messageConversations.clear();

                        for (Conversation conversation : list) {
                            messageConversations.add(new MessageConversation(conversation));
                        }
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
                LogUtils.w("ConversationPickerActivity", throwable);
            }
        }).launch();
    }

    protected class ConversationAdapter extends AdapterForRecyclerView<MessageConversation> {

        private ConversationPickerActivity activity;

        public ConversationAdapter() {
            super(ConversationPickerActivity.this, messageConversations, R.layout.item_simple_conversation);
            this.activity = ConversationPickerActivity.this;
        }

        @Override
        public void convert(ViewHolderForRecyclerView helper, MessageConversation item, int position) {
            ImageView avatar = helper.getView(R.id.ivAvatar);

            if (item.conversation.getType() == ConversationType.Contact) {
                avatar.setImageResource(item.avatarResourceId);
                helper.setText(R.id.tvDisplayName, item.conversation.getContact().getPriorityName());
            }
            else if (item.conversation.getType() == ConversationType.Group) {
                AvatarUtils.fillGroupAvatar(activity, item.conversation.getGroup(), avatar);
                helper.setText(R.id.tvDisplayName, item.conversation.getGroup().getPriorityName());
            }

            // 会话是否置顶
            if (item.conversation.focused()) {
                helper.getView(R.id.llRoot).setBackgroundColor(UIUtils.getColorByAttrId(R.attr.colorThemeBackground));
            }
            else {
                helper.getView(R.id.llRoot).setBackgroundColor(UIUtils.getColorByAttrId(R.attr.colorBackground));
            }

            if (singleMode) {
                helper.setViewVisibility(R.id.cbSelector, View.GONE);
            }
            else {
                helper.setViewVisibility(R.id.cbSelector, View.VISIBLE);
            }
        }
    }
}
