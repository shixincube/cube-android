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
import cube.contact.handler.DefaultGroupHandler;
import cube.contact.model.Group;
import cube.contact.model.GroupAppendix;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;
import cube.engine.util.Future;
import cube.engine.util.Promise;
import cube.engine.util.PromiseFuture;
import cube.engine.util.PromiseHandler;
import cube.messaging.handler.DefaultConversationHandler;
import cube.messaging.model.Conversation;
import cube.messaging.model.ConversationReminding;
import cube.messaging.model.ConversationType;
import cube.util.LogUtils;

/**
 * 会话详情。
 */
public class ConversationDetailsActivity extends BaseActivity<ConversationDetailsView, ConversationDetailsPresenter> implements ConversationDetailsView {

    private final static String TAG = ConversationDetailsActivity.class.getSimpleName();

    public final static int REQUEST_CREATE_OR_UPDATE_GROUP = 1000;
    public final static int REQUEST_ADD_GROUP_MEMBER = 1001;
    public final static int REQUEST_REMOVE_GROUP_MEMBER = 1002;
    public final static int REQUEST_SET_GROUP_NAME = 1100;
    public final static int REQUEST_SET_GROUP_NOTICE = 1200;

    public final static int RESULT_INVALIDATE = 2000;
    public final static int RESULT_DESTROY = 3000;

    @BindView(R.id.rvMembers)
    RecyclerView membersRecyclerView;

    @BindView(R.id.llGroupDetails)
    LinearLayout groupDetailsLayout;

    @BindView(R.id.llGroupOperation)
    LinearLayout groupOperationLayout;

    @BindView(R.id.oivGroupName)
    OptionItemView groupNameItemView;

    @BindView(R.id.llGroupNotice)
    LinearLayout groupNoticeLayout;
    @BindView(R.id.tvGroupNotice)
    TextView groupNoticeView;

    @BindView(R.id.oivRemarkGroup)
    OptionItemView groupRemarkItemView;

    @BindView(R.id.oivSearchContent)
    OptionItemView searchContent;

    @BindView(R.id.sbCloseRemind)
    SwitchButton closeRemindSwitch;

    @BindView(R.id.sbTopConversation)
    SwitchButton topConversationSwitch;

    @BindView(R.id.oivNameInGroup)
    OptionItemView nameInGroupItemView;

    @BindView(R.id.sbSaveAsGroup)
    SwitchButton saveAsGroupSwitch;

    @BindView(R.id.sbDisplayMemberName)
    SwitchButton displayMemberNameSwitch;

    @BindView(R.id.btnClearRecords)
    Button clearRecordsButton;

    @BindView(R.id.btnQuitGroup)
    Button quitGroupButton;

    private boolean isFirst = true;

    private Conversation conversation;

    public ConversationDetailsActivity() {
        super();
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.presenter.load();
    }

    @Override
    public void init() {
        Intent intent = getIntent();
        long conversationId = intent.getLongExtra("conversationId", 0);
        this.conversation = CubeEngine.getInstance().getMessagingService().getConversation(conversationId);
    }

    @Override
    public void initView() {
        this.setToolbarTitle(UIUtils.getString(R.string.conv_title));

        if (this.conversation.getType() == ConversationType.Contact) {
            this.groupDetailsLayout.setVisibility(View.GONE);
            this.groupOperationLayout.setVisibility(View.GONE);
            this.quitGroupButton.setVisibility(View.GONE);
        }
        else if (this.conversation.getType() == ConversationType.Group) {
            this.groupNameItemView.setEndText(this.conversation.getDisplayName());
            Group group = this.conversation.getGroup();
            GroupAppendix appendix = group.getAppendix();

            // 公告
            if (appendix.hasNotice()) {
                this.groupNoticeView.setText(appendix.getNotice());
            }
            else {
                this.groupNoticeView.setVisibility(View.GONE);
            }

            // 备注
            if (appendix.hasRemark()) {
                this.groupRemarkItemView.setEndText(appendix.getRemark());
            }
        }
    }

    @Override
    public void initListener() {
        // 查找消息内容
        this.searchContent.setOnClickListener((view) -> {
            UIUtils.showToast(UIUtils.getString(R.string.developing));
        });

        // 关闭消息提醒按钮事件
        this.closeRemindSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    if (conversation.getReminding() == ConversationReminding.Closed) {
                        return;
                    }
                }
                else {
                    if (conversation.getReminding() == ConversationReminding.Normal) {
                        return;
                    }
                }

                Promise.create(new PromiseHandler<Boolean>() {
                    @Override
                    public void emit(PromiseFuture<Boolean> promise) {
                        boolean result = CubeEngine.getInstance().getMessagingService().updateConversation(conversation,
                                checked ? ConversationReminding.Closed : ConversationReminding.Normal);
                        promise.resolve(result);
                    }
                }).then(new Future<Boolean>() {
                    @Override
                    public void come(Boolean data) {
                        if (data.booleanValue()) {
                            LogUtils.i(TAG, "Change reminding success : " + conversation.getReminding().name());
                        }
                        else {
                            LogUtils.w(TAG, "Change reminding failure : " + conversation.getReminding().name());
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

        // 群组名称
        this.groupNameItemView.setOnClickListener((view) -> {
            Intent intent = new Intent(ConversationDetailsActivity.this, SimpleContentInputActivity.class);
            intent.putExtra("title", UIUtils.getString(R.string.modify_group_name));
            intent.putExtra("tip", UIUtils.getString(R.string.modify_group_name_tip_notify_all));
            intent.putExtra("content", conversation.getDisplayName());
            intent.putExtra("groupId", conversation.getGroup().getId());
            startActivityForResult(intent, REQUEST_SET_GROUP_NAME);
        });

        // 群组公告
        this.groupNoticeLayout.setOnClickListener((view) -> {
            if (conversation.getGroup().isOwner()) {
                Intent intent = new Intent(ConversationDetailsActivity.this, TextInputActivity.class);
                intent.putExtra("title", UIUtils.getString(R.string.modify_group_notice));
                if (conversation.getGroup().getAppendix().hasNotice()) {
                    intent.putExtra("content", conversation.getGroup().getAppendix().getNotice());
                }
                startActivityForResult(intent, REQUEST_SET_GROUP_NOTICE);
            }
            else {
                if (conversation.getGroup().getAppendix().hasNotice()) {
                    Intent intent = new Intent(ConversationDetailsActivity.this, GroupNoticeActivity.class);
                    intent.putExtra("groupId", conversation.getGroup().getId());
                    startActivity(intent);
                }
                else {
                    UIUtils.showToast(UIUtils.getString(R.string.no_group_notice));
                }
            }
        });

        // 群组备注
        this.groupRemarkItemView.setOnClickListener((view) -> {
            UIUtils.showToast(UIUtils.getString(R.string.developing));
        });

        // 将多人会话保存为群组
        this.saveAsGroupSwitch.setOnCheckedChangeListener((CompoundButton compoundButton, boolean checked) -> {
            if (checked) {
                presenter.saveToGroupZone();
            }
            else {
                presenter.dropFromGroupZone();
            }
        });

        // 在本群里的昵称
        this.nameInGroupItemView.setOnClickListener((view) -> {
            UIUtils.showToast(UIUtils.getString(R.string.developing));
        });

        // 显示群成员名称
        this.displayMemberNameSwitch.setOnCheckedChangeListener((CompoundButton compoundButton, boolean checked) -> {
            Group group = conversation.getGroup();
            if (group.getAppendix().isDisplayMemberName() == checked) {
                return;
            }

            group.getAppendix().modifyDisplayNameFlag(checked, new DefaultGroupHandler(true) {
                @Override
                public void handleGroup(Group group) {
                    UIUtils.showToast(UIUtils.getString(R.string.operate_success));

                    // 需要设置结果，以便消息面板更新消息的 ITEM 显示
                    setResult(RESULT_INVALIDATE);
                }
            }, new DefaultFailureHandler(true) {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    UIUtils.showToast(UIUtils.getString(R.string.operate_failure));
                }
            });
        });

        // 清空消息记录按钮事件
        this.clearRecordsButton.setOnClickListener((view) -> {
            presenter.clearAllMessages();
        });

        // 删除并退出按钮事件
        this.quitGroupButton.setOnClickListener((view) -> {
            presenter.quitGroup();
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
        else if (requestCode == REQUEST_ADD_GROUP_MEMBER) {
            if (resultCode == RESULT_OK) {
                // 添加联系人
                long[] idList = data.getLongArrayExtra("members");
                presenter.addMembers(idList);
            }
        }
        else if (requestCode == REQUEST_REMOVE_GROUP_MEMBER) {
            if (resultCode == RESULT_OK) {
                // 移除联系人
                long[] idList = data.getLongArrayExtra("members");
                presenter.removeMembers(idList);
            }
        }
        else if (requestCode == REQUEST_SET_GROUP_NAME) {
            if (resultCode == RESULT_OK) {
                UIUtils.showToast(UIUtils.getString(R.string.change_group_name));
                String content = data.getStringExtra("content");
                // 修改群聊名称
                CubeEngine.getInstance().getMessagingService().changeConversationName(conversation,
                        content, new DefaultConversationHandler(true) {
                            @Override
                            public void handleConversation(Conversation conversation) {
                                groupNameItemView.setEndText(conversation.getDisplayName());
                            }
                        }, new DefaultFailureHandler(true) {
                            @Override
                            public void handleFailure(Module module, ModuleError error) {
                                UIUtils.showToast(UIUtils.getString(R.string.please_try_again_later));
                            }
                        });
            }
        }
        else if (requestCode == REQUEST_SET_GROUP_NOTICE) {
            if (resultCode == RESULT_OK) {
                String content = data.getStringExtra("content");
                this.conversation.getGroup().getAppendix().modifyNotice(content, new DefaultGroupHandler(true) {
                    @Override
                    public void handleGroup(Group group) {
                        groupNoticeView.setText(group.getAppendix().getNotice());
                        groupNoticeView.setVisibility(View.VISIBLE);
                    }
                }, new DefaultFailureHandler(true) {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {
                        UIUtils.showToast(UIUtils.getString(R.string.modify_group_notice_failure));
                    }
                });
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
    public SwitchButton getCloseRemindSwitchButton() {
        return this.closeRemindSwitch;
    }

    @Override
    public SwitchButton getTopConversationSwitchButton() {
        return this.topConversationSwitch;
    }

    @Override
    public SwitchButton getSaveAsGroupSwitchButton() {
        return this.saveAsGroupSwitch;
    }

    @Override
    public SwitchButton getDisplayMemberNameSwitchButton() {
        return this.displayMemberNameSwitch;
    }
}
