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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseFragmentActivity;
import com.shixincube.app.ui.presenter.MessagePanelPresenter;
import com.shixincube.app.ui.view.MessagePanelView;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.emotion.EmotionLayout;
import com.shixincube.app.widget.keyboard.SoftwareKeyboard;
import com.shixincube.app.widget.recyclerview.RecyclerView;
import com.shixincube.filepicker.Constant;
import com.shixincube.filepicker.activity.BaseActivity;
import com.shixincube.filepicker.activity.NormalFilePickActivity;
import com.shixincube.filepicker.filter.entity.NormalFile;
import com.shixincube.imagepicker.ImagePicker;
import com.shixincube.imagepicker.bean.ImageItem;
import com.shixincube.imagepicker.ui.ImageGridActivity;
import com.shixincube.imagepicker.ui.ImagePreviewActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import butterknife.BindView;
import cn.bingoogolapple.refreshlayout.BGANormalRefreshViewHolder;
import cn.bingoogolapple.refreshlayout.BGARefreshLayout;
import cube.engine.CubeEngine;
import cube.messaging.model.Conversation;
import cube.messaging.model.ConversationState;

/**
 * 消息面板。
 */
public class MessagePanelActivity extends BaseFragmentActivity<MessagePanelView, MessagePanelPresenter> implements MessagePanelView, BGARefreshLayout.BGARefreshLayoutDelegate {

    public final static int REQUEST_IMAGE_PICKER = 1000;
    public final static int REQUEST_TAKE_PHOTO = 2000;
    public final static int REQUEST_FILE_PICKER = 6000;

    public final static int REQUEST_DETAILS = 9000;

    @BindView(R.id.llMessagePanel)
    LinearLayout messagePanelLayout;

    @BindView(R.id.refreshLayout)
    BGARefreshLayout refreshLayout;

    @BindView(R.id.rvMessages)
    RecyclerView messageListView;

    @BindView(R.id.etContent)
    EditText inputContentView;

    @BindView(R.id.btnSend)
    Button sendButton;

    @BindView(R.id.ivEmoji)
    ImageView emojiButtonView;

    @BindView(R.id.ivMore)
    ImageView moreButtonView;

    @BindView(R.id.flFunctionView)
    FrameLayout functionView;
    @BindView(R.id.elEmotion)
    EmotionLayout emotionLayout;
    @BindView(R.id.llMore)
    LinearLayout moreLayout;

    @BindView(R.id.rlAlbum)
    RelativeLayout albumButton;
    @BindView(R.id.rlShot)
    RelativeLayout shotButton;
    @BindView(R.id.rlVoice)
    RelativeLayout voiceCallButton;
    @BindView(R.id.rlVideo)
    RelativeLayout videoCallButton;
    @BindView(R.id.rlFiles)
    RelativeLayout filesButton;

    private SoftwareKeyboard softwareKeyboard;

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
        this.toolbarMore.setImageResource(R.mipmap.ic_more_black);
        this.toolbarMore.setVisibility(View.VISIBLE);

        this.initRefreshLayout();
        this.initKeyboard();

        this.inputContentView.clearFocus();
    }

    private void initRefreshLayout() {
        this.refreshLayout.setDelegate(this);
        BGANormalRefreshViewHolder refreshViewHolder = new BGANormalRefreshViewHolder(this, false);
        refreshViewHolder.setRefreshingText(UIUtils.getString(R.string.loading_messages));
        refreshViewHolder.setPullDownRefreshText("");
        refreshViewHolder.setReleaseRefreshText("");
        this.refreshLayout.setRefreshViewHolder(refreshViewHolder);
    }

    @Override
    public void initData() {
        this.presenter.markAllRead();
        this.presenter.loadMessages();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void initListener() {
        // 更多按钮事件
        this.toolbarMore.setOnClickListener((view) -> {
            if (conversation.getState() == ConversationState.Deleted) {
                return;
            }

            Intent intent = new Intent(MessagePanelActivity.this, ConversationDetailsActivity.class);
            intent.putExtra("conversationId", this.conversation.getId());
            startActivityForResult(intent, REQUEST_DETAILS);
        });

        // 消息面板事件
        this.messagePanelLayout.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    closeBottomAndKeyboard();
                    break;
                default:
                    break;
            }
            return false;
        });
        this.messageListView.setOnTouchListener((view, event) -> {
            closeBottomAndKeyboard();
            return false;
        });

        // 输入框事件
        this.inputContentView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                // Nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
                if (inputContentView.getText().toString().trim().length() > 0) {
                    sendButton.setVisibility(View.VISIBLE);
                    moreButtonView.setVisibility(View.GONE);
                    // 发送正在输入消息提示
                    CubeEngine.getInstance().getMessagingService().sendTypingStatus(conversation);
                }
                else {
                    sendButton.setVisibility(View.GONE);
                    moreButtonView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Nothing
            }
        });
        inputContentView.setOnFocusChangeListener((view, focus) -> {
            if (focus) {
                UIUtils.postTaskDelay(() -> {
                    messageListView.smoothMoveToPosition(messageListView.getAdapter().getItemCount() - 1);
                }, 500);

                UIUtils.postTaskDelay(() -> {
                    if (!emotionLayout.isShown()) {
                        emojiButtonView.setImageResource(R.mipmap.message_tool_emotion);
                    }
                }, 100);
            }
        });

        // 发送按钮事件
        sendButton.setOnClickListener((view) -> {
            presenter.sendTextMessage();
        });

        // 相册按钮事件
        albumButton.setOnClickListener((view) -> {
            Intent intent = new Intent(this, ImageGridActivity.class);
            startActivityForResult(intent, REQUEST_IMAGE_PICKER);
        });
        shotButton.setOnClickListener((view) -> {
            Intent intent = new Intent(this, TakePhotoActivity.class);
            startActivityForResult(intent, REQUEST_TAKE_PHOTO);
        });
        // 文件按钮事件
        filesButton.setOnClickListener((view) -> {
            Intent intent = new Intent(this, NormalFilePickActivity.class);
            intent.putExtra(Constant.MAX_NUMBER, 9);
            intent.putExtra(BaseActivity.IS_NEED_FOLDER_LIST, true);
            intent.putExtra(NormalFilePickActivity.SUFFIX,
                    new String[] {"jpg", "jpeg", "png", "bmp",
                            "xlsx", "xls", "doc", "docx", "ppt", "pptx", "pdf"});
            startActivityForResult(intent, REQUEST_FILE_PICKER);
        });
    }

    private void initKeyboard() {
        this.softwareKeyboard = SoftwareKeyboard.with(this);
        this.softwareKeyboard.bindToEditText(this.inputContentView);
        this.softwareKeyboard.bindToContent(this.functionView);
        this.softwareKeyboard.bindToLayoutAndButton(this.emotionLayout, this.emojiButtonView);
        this.softwareKeyboard.bindToLayoutAndButton(this.moreLayout, this.moreButtonView);
        this.softwareKeyboard.setButtonOnClickListener(new SoftwareKeyboard.ButtonOnClickListener() {
            @Override
            public boolean onButtonClickListener(View view) {
                switch (view.getId()) {
                    case R.id.ivEmoji:
                        UIUtils.postTaskDelay(() -> messageListView.smoothMoveToPosition(messageListView.getAdapter().getItemCount() - 1), 100);
                        inputContentView.clearFocus();
                        if (emotionLayout.isShown()) {
                            emojiButtonView.setImageResource(R.mipmap.message_tool_emotion);
                        }
                        else {
                            emojiButtonView.setImageResource(R.mipmap.message_tool_keyboard);
                        }
                        break;
                    case R.id.ivMore:
                        UIUtils.postTaskDelay(() -> {
                            messageListView.smoothMoveToPosition(messageListView.getAdapter().getItemCount() - 1);
                            inputContentView.clearFocus();
                        }, 100);
                        inputContentView.clearFocus();
                        if (!moreLayout.isShown()) {
                            emojiButtonView.setImageResource(R.mipmap.message_tool_emotion);
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    private void closeBottomAndKeyboard() {
        this.emojiButtonView.setImageResource(R.mipmap.message_tool_emotion);
//        this.moreButtonView.setVisibility(View.VISIBLE);
//        this.sendButton.setVisibility(View.GONE);

        if (null != this.softwareKeyboard) {
            this.softwareKeyboard.interceptBackPress();
        }

        if (inputContentView.isShown()) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm.isActive() && null != getCurrentFocus()) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                if (getCurrentFocus() == inputContentView) {
                    inputContentView.clearFocus();
                }
            }
        }
    }

    public void fireClickBlank() {
        this.closeBottomAndKeyboard();
    }

    @Override
    public void onBackPressed() {
        if (emotionLayout.isShown() || moreLayout.isShown()) {
            this.softwareKeyboard.interceptBackPress();
            this.emojiButtonView.setImageResource(R.mipmap.message_tool_emotion);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        setToolbarTitle(this.conversation.getDisplayName());
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

        this.softwareKeyboard.destroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_IMAGE_PICKER:
                if (resultCode == ImagePicker.RESULT_CODE_ITEMS) {
                    // 返回多张照片
                    if (null != data) {
                        // 是否发送原图
                        boolean origen = data.getBooleanExtra(ImagePreviewActivity.ISORIGIN, false);
                        ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
                        for (ImageItem item : images) {
                            // 发送图片消息
                            presenter.sendImageMessage(new File(item.path), origen);
                        }
                    }
                }
                break;
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    String path = data.getStringExtra("path");
                    if (data.getBooleanExtra("photo", true)) {
                        // 拍照
                        presenter.sendImageMessage(new File(path), false);
                    }
                    else {
                        // 录制视频
                        // TODO
                    }
                }
                break;
            case REQUEST_FILE_PICKER:
                if (resultCode == RESULT_OK) {
                    ArrayList<NormalFile> list = data.getParcelableArrayListExtra(Constant.RESULT_PICK_FILE);
                    // TODO 文件处理
                }
                break;
            case REQUEST_DETAILS:
                if (resultCode == ConversationDetailsActivity.RESULT_INVALIDATE) {
                    // 需要更新消息界面
                    presenter.loadMessages();
                }
                else if (resultCode == ConversationDetailsActivity.RESULT_DESTROY) {
                    // 会话已经被删除
                    // 提示当前会话已失效
                    presenter.appendLocalMessage(UIUtils.getString(R.string.tip_conversation_invalid));
                }
                break;
            default:
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
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
        this.presenter.loadMoreMessages();
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

    @Override
    public BGARefreshLayout getRefreshLayout() {
        return this.refreshLayout;
    }
}
