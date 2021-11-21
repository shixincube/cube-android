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
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kyleduo.switchbutton.SwitchButton;
import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.presenter.ConversationDetailsPresenter;
import com.shixincube.app.ui.view.ConversationDetailsView;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.optionitemview.OptionItemView;
import com.shixincube.app.widget.recyclerview.RecyclerView;

import butterknife.BindView;
import cube.contact.model.Group;
import cube.contact.model.GroupAppendix;
import cube.engine.CubeEngine;
import cube.engine.util.Future;
import cube.engine.util.Promise;
import cube.engine.util.PromiseFuture;
import cube.engine.util.PromiseHandler;
import cube.messaging.model.Conversation;
import cube.messaging.model.ConversationReminded;
import cube.messaging.model.ConversationType;
import cube.util.LogUtils;

/**
 * 会话详情。
 */
public class ConversationDetailsActivity extends BaseActivity<ConversationDetailsView, ConversationDetailsPresenter> implements ConversationDetailsView {

    private final static String TAG = ConversationDetailsActivity.class.getSimpleName();

    public final static int REQUEST_CREATE_OR_UPDATE_GROUP = 1000;

    @BindView(R.id.tvToolbarTitle)
    TextView toolbarTitleTextView;

    @BindView(R.id.rvMembers)
    RecyclerView membersRecyclerView;

    @BindView(R.id.llGroupDetails)
    LinearLayout groupDetailsLayout;

    @BindView(R.id.llGroupOperation)
    LinearLayout groupOperationLayout;

    @BindView(R.id.oivGroupName)
    OptionItemView groupNameItemView;

    @BindView(R.id.tvGroupNotice)
    TextView groupNoticeView;

    @BindView(R.id.oivRemarkGroup)
    OptionItemView groupRemarkItemView;

    @BindView(R.id.sbCloseRemind)
    SwitchButton closeRemindSwitch;

    @BindView(R.id.sbTopConversation)
    SwitchButton topConversationSwitch;

    @BindView(R.id.btnQuitGroup)
    Button quitGroupButton;

    private boolean isFirst = true;

    private Conversation conversation;

    public ConversationDetailsActivity() {
        super();
    }

    @Override
    public void onResume() {
        super.onResume();

        this.presenter.load();
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

        if (this.conversation.getType() == ConversationType.Contact) {
            this.groupDetailsLayout.setVisibility(View.GONE);
            this.groupOperationLayout.setVisibility(View.GONE);
            this.quitGroupButton.setVisibility(View.GONE);
        }
        else if (this.conversation.getType() == ConversationType.Group) {
            this.groupNameItemView.setEndText(this.conversation.getDisplayName());
            Group group = this.conversation.getGroup();
            GroupAppendix appendix = group.getAppendix();
            if (appendix.hasNotice()) {
                this.groupNoticeView.setText(appendix.getNotice());
            }
            else {
                this.groupNoticeView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void initListener() {
        // 关闭消息提醒按钮事件
        this.closeRemindSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    if (conversation.getReminded() == ConversationReminded.Closed) {
                        return;
                    }
                }
                else {
                    if (conversation.getReminded() == ConversationReminded.Normal) {
                        return;
                    }
                }

                Promise.create(new PromiseHandler<Boolean>() {
                    @Override
                    public void emit(PromiseFuture<Boolean> promise) {
                        boolean result = CubeEngine.getInstance().getMessagingService().updateConversation(conversation,
                                checked ? ConversationReminded.Closed : ConversationReminded.Normal);
                        promise.resolve(result);
                    }
                }).then(new Future<Boolean>() {
                    @Override
                    public void come(Boolean data) {
                        if (data.booleanValue()) {
                            LogUtils.i(TAG, "Change reminding success : " + conversation.getReminded().name());
                        }
                        else {
                            LogUtils.w(TAG, "Change reminding failure : " + conversation.getReminded().name());
                        }
                    }
                }).launch();
            }
        });

        // 置顶按钮事件
        this.topConversationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (conversation.focused() == checked) {
                    return;
                }

                if (checked) {
                    // 设置置顶
                    Promise.create(new PromiseHandler<Boolean>() {
                        @Override
                        public void emit(PromiseFuture<Boolean> promise) {
                            boolean result = CubeEngine.getInstance().getMessagingService().focusOnConversation(conversation);
                            promise.resolve(result);
                        }
                    }).thenOnMainThread(new Future<Boolean>() {
                        @Override
                        public void come(Boolean data) {
                            if (data.booleanValue()) {
                                UIUtils.showToast(UIUtils.getString(R.string.conv_top_confirm));
                            }
                            else {
                                UIUtils.showToast(UIUtils.getString(R.string.please_try_again_later));
                            }
                        }
                    }).launch();
                }
                else {
                    // 取消置顶
                    Promise.create(new PromiseHandler<Boolean>() {
                        @Override
                        public void emit(PromiseFuture<Boolean> promise) {
                            boolean result = CubeEngine.getInstance().getMessagingService().focusOutConversation(conversation);
                            promise.resolve(result);
                        }
                    }).thenOnMainThread(new Future<Boolean>() {
                        @Override
                        public void come(Boolean data) {
                            if (data.booleanValue()) {
                                UIUtils.showToast(UIUtils.getString(R.string.conv_top_cancel));
                            }
                            else {
                                UIUtils.showToast(UIUtils.getString(R.string.please_try_again_later));
                            }
                        }
                    }).launch();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CREATE_OR_UPDATE_GROUP) {
            if (resultCode == RESULT_OK) {
                // 创建群聊成功，结束当前界面
                finish();
            }
        }
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
        return this.membersRecyclerView;
    }

    @Override
    public OptionItemView getGroupNameItemView() {
        return this.groupNameItemView;
    }

    @Override
    public SwitchButton getCloseRemindSwitchButton() {
        return this.closeRemindSwitch;
    }

    @Override
    public SwitchButton getTopConversationSwitchButton() {
        return this.topConversationSwitch;
    }
}
