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

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;
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

import androidx.annotation.NonNull;

import com.shixincube.app.CubeApp;
import com.shixincube.app.R;
import com.shixincube.app.model.MessageConversation;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.presenter.MessagePanelPresenter;
import com.shixincube.app.ui.view.MessagePanelView;
import com.shixincube.app.util.AvatarUtils;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.emotion.EmotionLayout;
import com.shixincube.app.widget.keyboard.SoftwareKeyboard;
import com.shixincube.app.widget.recyclerview.RecyclerView;
import com.shixincube.filepicker.Constant;
import com.shixincube.filepicker.activity.NormalFilePickActivity;
import com.shixincube.filepicker.filter.entity.NormalFile;
import com.shixincube.imagepicker.ImagePicker;
import com.shixincube.imagepicker.bean.ImageItem;
import com.shixincube.imagepicker.ui.ImageGridActivity;
import com.shixincube.imagepicker.ui.ImagePreviewActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import cn.bingoogolapple.refreshlayout.BGANormalRefreshViewHolder;
import cn.bingoogolapple.refreshlayout.BGARefreshLayout;
import cube.contact.model.Contact;
import cube.contact.model.Group;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;
import cube.engine.service.FloatingVideoWindowBinder;
import cube.engine.service.FloatingVideoWindowListener;
import cube.engine.service.FloatingVideoWindowService;
import cube.messaging.model.Conversation;
import cube.messaging.model.ConversationState;
import cube.messaging.model.ConversationType;
import cube.multipointcomm.handler.DefaultCommFieldHandler;
import cube.multipointcomm.model.CommField;
import cube.multipointcomm.util.MediaConstraint;
import cube.util.LogUtils;
import kr.co.namee.permissiongen.PermissionFail;
import kr.co.namee.permissiongen.PermissionGen;
import kr.co.namee.permissiongen.PermissionSuccess;

/**
 * 消息面板。
 */
public class MessagePanelActivity extends BaseActivity<MessagePanelView, MessagePanelPresenter> implements MessagePanelView, BGARefreshLayout.BGARefreshLayoutDelegate, ServiceConnection {

    private final static String TAG = MessagePanelActivity.class.getSimpleName();

    public final static int REQUEST_IMAGE_PICKER = 1000;
    public final static int REQUEST_TAKE_PHOTO = 2000;
    public final static int REQUEST_FILE_PICKER = 6000;

    public final static int REQUEST_DETAILS = 8000;

    public final static int REQUEST_OVERLAY_PERMISSION = 9000;
    public final static int REQUEST_AUDIO_VIDEO_PERMISSION = 9100;

    public final static int REQUEST_SELECT_GROUP_MEMBERS_FOR_CALL = 9500;
    public final static int REQUEST_SELECT_GROUP_MEMBERS_FOR_INVITE = 9600;

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

    @BindView(R.id.ivBurnMode)
    ImageView burnButtonView;

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
    @BindView(R.id.rlAudio)
    RelativeLayout audioCallButton;
    @BindView(R.id.rlVideo)
    RelativeLayout videoCallButton;
    @BindView(R.id.rlFiles)
    RelativeLayout filesButton;

    private FloatingVideoWindowBinder binder;

    private SoftwareKeyboard softwareKeyboard;

    private Conversation conversation;

    private MediaConstraint mediaConstraint;

    public MessagePanelActivity() {
        super();
    }

    @Override
    public void init() {
        Intent intent = getIntent();
        long conversationId = intent.getLongExtra("conversationId", 0);
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

        this.presenter.refreshState();

        runOnUiThread(() -> {
            // 绑定视频通话的监听器
            Intent serviceIntent = new Intent(this, FloatingVideoWindowService.class);
            bindService(serviceIntent, MessagePanelActivity.this, BIND_AUTO_CREATE);
        });
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
                    CubeEngine.getInstance().getMessagingService().startTypingStatus(conversation);
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

        // 阅后即焚模式切换
        burnButtonView.setOnClickListener((view) -> {

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

        // 拍照按钮事件
        shotButton.setOnClickListener((view) -> {
            Intent intent = new Intent(this, TakePhotoActivity.class);
            startActivityForResult(intent, REQUEST_TAKE_PHOTO);
        });

        // 语音通话按钮事件
        audioCallButton.setOnClickListener((view) -> {
            mediaConstraint = new MediaConstraint(false);
            if (!Settings.canDrawOverlays(this)) {
                UIUtils.showToast(UIUtils.getString(R.string.apply_overlay_permission));
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())), REQUEST_OVERLAY_PERMISSION);
            }
            else {
                requestAudioVideoWindow();
            }
        });

        // 视频通话按钮事件
        videoCallButton.setOnClickListener((view) -> {
            mediaConstraint = new MediaConstraint(true);
            if (!Settings.canDrawOverlays(this)) {
                UIUtils.showToast(UIUtils.getString(R.string.apply_overlay_permission));
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())), REQUEST_OVERLAY_PERMISSION);
            }
            else {
                requestAudioVideoWindow();
            }
        });

        // 文件按钮事件
        filesButton.setOnClickListener((view) -> {
            Intent intent = new Intent(this, NormalFilePickActivity.class);
            intent.putExtra(Constant.MAX_NUMBER, 9);
            intent.putExtra(com.shixincube.filepicker.activity.BaseActivity.IS_NEED_FOLDER_LIST, true);
            intent.putExtra(NormalFilePickActivity.SUFFIX,
                    new String[] {"jpg", "jpeg", "png", "bmp",
                            "xlsx", "xls", "doc", "docx", "ppt", "pptx", "pdf"});
            startActivityForResult(intent, REQUEST_FILE_PICKER);
        });
    }

    private void requestAudioVideoWindow() {
        PermissionGen.with(this)
            .addRequestCode(REQUEST_AUDIO_VIDEO_PERMISSION)
            .permissions(
                    // 摄像机权限
                    Manifest.permission.CAMERA,
                    // 麦克风权限
                    Manifest.permission.RECORD_AUDIO,
                    // 扬声器权限
                    Manifest.permission.MODIFY_AUDIO_SETTINGS
            )
            .request();
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
                        UIUtils.postTaskDelay(() -> {
                            messageListView.smoothMoveToPosition(messageListView.getAdapter().getItemCount() - 1);
                            inputContentView.clearFocus();
                        }, 100);
                        inputContentView.clearFocus();
                        if (emotionLayout.isShown()) {
                            emojiButtonView.setImageResource(R.mipmap.message_tool_emotion);
                            // 恢复输入法软键盘
                            softwareKeyboard.showSoftInput();
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

            if (null != MessageConversation.ActiveConversation) {
                if (MessageConversation.ActiveConversation.equals(this.conversation)) {
                    MessageConversation.ActiveConversation = null;
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        setToolbarTitle(this.conversation.getDisplayName());

        MessageConversation.ActiveConversation = this.conversation;
    }

    @Override
    public void onDestroy() {
        unbindService(this);

        super.onDestroy();

        if (null != this.presenter) {
            try {
                this.presenter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.softwareKeyboard.destroy();

        if (null != MessageConversation.ActiveConversation) {
            if (MessageConversation.ActiveConversation.equals(this.conversation)) {
                MessageConversation.ActiveConversation = null;
            }
        }
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
            case REQUEST_OVERLAY_PERMISSION:
                if (!Settings.canDrawOverlays(MessagePanelActivity.this)) {
                    UIUtils.showToast(UIUtils.getString(R.string.auth_overlay_permission_failure));
                }
                else {
                    requestAudioVideoWindow();
                }
                break;
            case REQUEST_AUDIO_VIDEO_PERMISSION:
                if (getPackageManager().checkPermission("android.permission.CAMERA", "packageName")
                        == PackageManager.PERMISSION_GRANTED) {
                    onPermissionSuccess();
                }
                break;
            case REQUEST_SELECT_GROUP_MEMBERS_FOR_CALL:
                if (resultCode == RESULT_OK) {
                    // 发起群组通话邀请
                    long[] memberIds = data.getLongArrayExtra("members");
                    Intent intent = new Intent(MessagePanelActivity.this, FloatingVideoWindowService.class);
                    intent.setAction(FloatingVideoWindowService.ACTION_SHOW_INVITER);
                    intent.putExtra("groupId", conversation.getGroup().getId().longValue());
                    intent.putExtra("invitees", memberIds);
                    intent.putExtra("mediaConstraint", this.mediaConstraint.toJSON().toString());
                    startService(intent);
                }
                break;
            case REQUEST_SELECT_GROUP_MEMBERS_FOR_INVITE:
                if (resultCode == RESULT_OK) {
                    // 在通话时邀请新的参与人
                    long[] memberIds = data.getLongArrayExtra("members");
                    long groupId = data.getLongExtra("groupId", 0);
                    Group group = CubeEngine.getInstance().getContactService().getGroup(groupId);

                    List<Contact> participants = this.binder.getService().getParticipants();
                    List<Contact> newParticipants = new ArrayList<>();
                    for (long id : memberIds) {
                        Contact contact = CubeEngine.getInstance().getContactService().getContact(id);
                        if (!participants.contains(contact)) {
                            newParticipants.add(contact);
                        }
                    }

                    if (LogUtils.isDebugLevel()) {
                        LogUtils.d(TAG, "#onActivityResult - REQUEST_SELECT_GROUP_MEMBERS_FOR_INVITE : "
                                + newParticipants.size());
                    }

                    CubeEngine.getInstance().getMultipointComm().inviteCall(group, newParticipants,
                            new DefaultCommFieldHandler(true) {
                        @Override
                        public void handleCommField(CommField commField) {
                            if (LogUtils.isDebugLevel()) {
                                LogUtils.d(TAG, "#inviteCall - REQUEST_SELECT_GROUP_MEMBERS_FOR_INVITE");
                            }
                        }
                    }, new DefaultFailureHandler(true) {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            LogUtils.w(TAG, "#inviteCall - REQUEST_SELECT_GROUP_MEMBERS_FOR_INVITE : " + error.code);
                        }
                    });
                }

                // 恢复界面
                this.binder.getService().resumeDisplay();
                break;
            default:
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionGen.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @PermissionSuccess(requestCode = REQUEST_AUDIO_VIDEO_PERMISSION)
    public void onPermissionSuccess() {
        if (conversation.getType() == ConversationType.Contact) {
            Intent intent = new Intent(MessagePanelActivity.this, FloatingVideoWindowService.class);
            intent.setAction(FloatingVideoWindowService.ACTION_SHOW_CALLER);
            intent.putExtra("contactId", conversation.getContact().getId().longValue());
            intent.putExtra("avatarResource", AvatarUtils.getAvatarResource(conversation.getContact()));
            intent.putExtra("mediaConstraint", this.mediaConstraint.toJSON().toString());
            startService(intent);
        }
        else if (conversation.getType() == ConversationType.Group) {
            Intent intent = new Intent(MessagePanelActivity.this, OperateContactActivity.class);
            intent.putExtra("groupId", conversation.getGroup().getId().longValue());
            intent.putExtra("onlyThisGroup", true);
            intent.putExtra("maxSelectedNum", this.mediaConstraint.videoEnabled ? 5 : 8);
            startActivityForResult(intent, REQUEST_SELECT_GROUP_MEMBERS_FOR_CALL);
        }
    }

    @PermissionFail(requestCode = REQUEST_AUDIO_VIDEO_PERMISSION)
    public void onPermissionFail() {
        UIUtils.showToast(UIUtils.getString(R.string.apply_camera_permission));

        startActivityForResult(new Intent(Settings.ACTION_PRIVACY_SETTINGS), REQUEST_AUDIO_VIDEO_PERMISSION);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        this.binder = (FloatingVideoWindowBinder) iBinder;
        this.binder.setListener(new FloatingVideoWindowListener() {
            @Override
            public void onInviteClick(View button, FloatingVideoWindowService service, Group group, List<Contact> participantList) {
                long[] memberIdList = new long[participantList.size()];
                for (int i = 0; i < memberIdList.length; ++i) {
                    memberIdList[i] = participantList.get(i).getId().longValue();
                }
                Intent intent = new Intent(MessagePanelActivity.this, OperateContactActivity.class);
                intent.putExtra("groupId", group.getId().longValue());
                intent.putExtra("onlyThisGroup", true);
                intent.putExtra("lockedIdList", memberIdList);
                intent.putExtra("maxSelectedNum", service.getMediaConstraint().videoEnabled ? 5 : 8);
                startActivityForResult(intent, REQUEST_SELECT_GROUP_MEMBERS_FOR_INVITE);

                // 挂起
                CubeApp.getMainThreadHandler().postDelayed(() -> {
                    service.suspendDisplay();
                }, 500);
            }
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        if (null != this.binder) {
            this.binder.setListener(null);
            this.binder = null;
        }
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

    @Override
    public ImageView getBurnButtonView() {
        return this.burnButtonView;
    }
}
