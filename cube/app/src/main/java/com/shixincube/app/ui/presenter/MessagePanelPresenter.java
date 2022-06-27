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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.bumptech.glide.Glide;
import com.shixincube.app.AppConsts;
import com.shixincube.app.CubeApp;
import com.shixincube.app.R;
import com.shixincube.app.ui.activity.ImageShowcaseActivity;
import com.shixincube.app.ui.activity.MessagePanelActivity;
import com.shixincube.app.ui.adapter.MessagePanelAdapter;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.MessagePanelView;
import com.shixincube.app.util.FileOpenUtils;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.adapter.OnItemClickListener;
import com.shixincube.app.widget.adapter.ViewHolder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.core.handler.StableFailureHandler;
import cube.engine.CubeEngine;
import cube.engine.util.Future;
import cube.engine.util.Promise;
import cube.engine.util.PromiseFuture;
import cube.engine.util.PromiseHandler;
import cube.messaging.MessageEventListener;
import cube.messaging.MessageListResult;
import cube.messaging.MessagingService;
import cube.messaging.MessagingServiceState;
import cube.messaging.extension.BurnMessage;
import cube.messaging.extension.FileMessage;
import cube.messaging.extension.HyperTextMessage;
import cube.messaging.extension.ImageMessage;
import cube.messaging.extension.NotificationMessage;
import cube.messaging.extension.VoiceMessage;
import cube.messaging.handler.DefaultConversationHandler;
import cube.messaging.handler.DefaultEraseMessageHandler;
import cube.messaging.handler.DefaultMessageHandler;
import cube.messaging.handler.DefaultMessageListResultHandler;
import cube.messaging.handler.DefaultSendHandler;
import cube.messaging.handler.SimpleSendHandler;
import cube.messaging.model.Conversation;
import cube.messaging.model.ConversationState;
import cube.messaging.model.ConversationType;
import cube.messaging.model.Message;
import cube.messaging.model.MessageState;
import cube.util.LogUtils;

/**
 * 消息面板。
 */
public class MessagePanelPresenter extends BasePresenter<MessagePanelView>
        implements OnItemClickListener, MessageEventListener {

    private final static String TAG = MessagePanelPresenter.class.getSimpleName();

    /**
     * 当前活跃的消息会话。
     */
    public static Conversation ActiveConversation = null;

    private int pageSize = 10;

    private boolean hasMoreMessage = true;

    private MessagePanelAdapter adapter;

    private Conversation conversation;

    private List<Message> messageList;

    private boolean burnMode;

    private boolean voiceInputMode;

    private long lastTouchTime = 0;

    private SimpleDateFormat simpleDateFormat;

    public MessagePanelPresenter(BaseActivity activity, Conversation conversation) {
        super(activity);
        this.conversation = conversation;
        this.messageList = new LinkedList<>();
        this.burnMode = false;
        this.voiceInputMode = false;
        this.simpleDateFormat = new SimpleDateFormat("MM月dd日 HH时mm分", Locale.CHINA);
        CubeEngine.getInstance().getMessagingService().addEventListener(conversation, this);
    }

    public boolean isBurnMode() {
        return this.burnMode;
    }

    public boolean isVoiceInputMode() {
        return this.voiceInputMode;
    }

    public void refreshState() {
        this.burnMode = this.conversation.getBoolean(AppConsts.BURN_MODE, false);

        if (this.burnMode) {
            getView().getBurnButtonView().setImageResource(R.mipmap.message_tool_burn_enable);
        }
        else {
            getView().getBurnButtonView().setImageResource(R.mipmap.message_tool_burn_disable);
        }

        this.voiceInputMode = this.conversation.getBoolean(AppConsts.VOICE_INPUT_MODE, false);

        if (this.voiceInputMode) {
            getView().getVoiceButtonView().setImageResource(R.mipmap.message_tool_keyboard);
            getView().getRecordVoiceButton().setVisibility(View.VISIBLE);
            getView().getInputContentView().setVisibility(View.GONE);
        }
        else {
            getView().getVoiceButtonView().setImageResource(R.mipmap.message_tool_voice);
            getView().getRecordVoiceButton().setVisibility(View.GONE);
            getView().getInputContentView().setVisibility(View.VISIBLE);
        }
    }

    public void markAllRead() {
        CubeEngine.getInstance().getMessagingService().markRead(this.conversation,
                new DefaultConversationHandler(false) {
            @Override
            public void handleConversation(Conversation conversation) {
                // Nothing
            }
        }, new DefaultFailureHandler(false) {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                // Nothing
            }
        });
    }

    public void loadMessages() {
        this.messageList.clear();
        MessageListResult result = CubeEngine.getInstance().getMessagingService()
                .getRecentMessages(this.conversation, this.pageSize);
        this.messageList.addAll(result.getList());
        this.hasMoreMessage = result.hasMore();

        if (null == this.adapter) {
            this.adapter = new MessagePanelAdapter(activity, this.messageList, this);
            this.adapter.setOnItemClickListener(this);

            getView().getMessageListView().setAdapter(this.adapter);

            UIUtils.postTaskDelay(() -> moveToBottom(), 300);
        }
        else {
            this.adapter.notifyDataSetChangedWrapper();
            moveToBottom();
        }

        if (this.messageList.isEmpty()) {
            // 提示
            String tip = null;
            if (this.conversation.getType() == ConversationType.Contact) {
                tip = UIUtils.getString(R.string.tip_new_contact_first, this.conversation.getDisplayName());
            }
            else if (this.conversation.getType() == ConversationType.Group) {
                tip = UIUtils.getString(R.string.tip_you_can_chat_in_this_group, this.conversation.getDisplayName());
            }

            if (null != tip) {
                this.appendLocalMessage(tip);
            }
        }
    }

    public void loadMoreMessages() {
        if (!this.hasMoreMessage) {
            // 已经没有更多消息可加载
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getView().getRefreshLayout().endRefreshing();
                }
            });
            return;
        }

        Message message = null;
        synchronized (this.messageList) {
            if (!this.messageList.isEmpty()) {
                message = this.messageList.get(0);
            }
        }

        int max = this.pageSize + this.pageSize;
        CubeEngine.getInstance().getMessagingService().queryMessages(conversation, message, max,
                new DefaultMessageListResultHandler(true) {
                    @Override
                    public void handleMessageList(List<Message> newMessageList, boolean hasMore) {
                        // 结束刷新
                        getView().getRefreshLayout().endRefreshing();

                        hasMoreMessage = hasMore;

                        if (newMessageList.isEmpty()) {
                            return;
                        }

                        // Result 清单里的消息是从旧到新的，时间正序，因此倒着插入到列表
                        synchronized (messageList) {
                            for (int i = newMessageList.size() - 1; i >= 0; --i) {
                                messageList.add(0, newMessageList.get(i));
                            }
                        }

                        getView().getMessageListView().moveToPosition(newMessageList.size() - 1);
                    }
                });
    }

    private boolean checkState() {
        if (this.conversation.getState() == ConversationState.Destroyed) {
            return false;
        }

        // 如果 House 失效，不允许发送消息
        if (AppConsts.FERRY_MODE) {
            if (!CubeEngine.getInstance().getFerryService().isHouseOnline()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 向本地追加消息。
     *
     * @param text 通知消息文本。
     */
    public void appendLocalMessage(String text) {
        Promise.create(new PromiseHandler<NotificationMessage>() {
            @Override
            public void emit(PromiseFuture<NotificationMessage> promise) {
                NotificationMessage notificationMessage = new NotificationMessage(text);
                CubeEngine.getInstance().getMessagingService().appendMessage(conversation,
                        notificationMessage);
                promise.resolve(notificationMessage);
            }
        }).thenOnMainThread(new Future<NotificationMessage>() {
            @Override
            public void come(NotificationMessage data) {
                adapter.addLastItem(data);
                moveToBottom();
            }
        }).launch();
    }

    public void sendTextMessage() {
        if (!this.checkState()) {
            UIUtils.showToast(UIUtils.getString(R.string.tip_conversation_invalid));
            return;
        }

        String text = getView().getInputContentView().getText().toString().trim();
        this.sendTextMessage(text);
        getView().getInputContentView().setText("");
    }

    public void sendTextMessage(String text) {
        if (!this.checkState()) {
            UIUtils.showToast(UIUtils.getString(R.string.tip_conversation_invalid));
            return;
        }

        if (this.burnMode) {
            BurnMessage burnMessage = new BurnMessage(text);
            CubeEngine.getInstance().getMessagingService().sendMessage(conversation, burnMessage,
                    new SimpleSendHandler<Conversation, BurnMessage>() {
                        @Override
                        public void handleSending(Conversation destination, BurnMessage message) {
                            // 将消息添加到界面
                            adapter.addLastItem(message);
                            moveToBottom();
                        }

                        @Override
                        public void handleSent(Conversation destination, BurnMessage message) {
                            // Nothing
                        }
                    }, new DefaultFailureHandler(true) {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            updateMessageStatus(burnMessage);
                        }
                    });
        }
        else {
            HyperTextMessage textMessage = new HyperTextMessage(text);
            CubeEngine.getInstance().getMessagingService().sendMessage(conversation, textMessage,
                    new SimpleSendHandler<Conversation, HyperTextMessage>() {
                        @Override
                        public void handleSending(Conversation destination, HyperTextMessage message) {
                            // 将消息添加到界面
                            adapter.addLastItem(message);
                            moveToBottom();
                        }

                        @Override
                        public void handleSent(Conversation destination, HyperTextMessage message) {
                            // Nothing
                        }
                    }, new DefaultFailureHandler(true) {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            // 消息错误，更新显示状态
                            updateMessageStatus(textMessage);
                        }
                    });
        }
    }

    public void sendFileMessage(File file) {
        if (!this.checkState()) {
            UIUtils.showToast(UIUtils.getString(R.string.tip_conversation_invalid));
            return;
        }

        FileMessage fileMessage = new FileMessage(file);
        CubeEngine.getInstance().getMessagingService().sendMessage(conversation, fileMessage,
                new DefaultSendHandler<Conversation, FileMessage>(true) {
                @Override
                public void handleProcessing(Conversation destination, FileMessage message) {
                    // 将消息添加到界面
                    adapter.addLastItem(message);
                    moveToBottom();
                }

                @Override
                public void handleProcessed(Conversation destination, FileMessage message) {
                    // Nothing
                }

                @Override
                public void handleSending(Conversation destination, FileMessage message) {
                    long processedSize = message.getProcessedSize();
                    if (processedSize >= 0) {
                        LogUtils.d(TAG, "#sendFileMessage - handleSending : " +
                                processedSize + "/" + message.getFileSize());
                    }
                }

                @Override
                public void handleSent(Conversation destination, FileMessage message) {
                    LogUtils.d(this.getClass().getSimpleName(), "#sendFileMessage - handleSent");

                    // 更新状态
                    updateMessageStatus(message);
                }
            }, new DefaultFailureHandler(true) {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    LogUtils.i(TAG, "#sendFileMessage - handleFailure : " + error.code);

                    // 更新状态
                    updateMessageStatus(fileMessage);
                }
            });
    }

    /**
     * 发送图片文件。
     *
     * @param file
     * @param useRawImage 是否使用原图。
     */
    public void sendImageMessage(File file, boolean useRawImage) {
        if (!this.checkState()) {
            UIUtils.showToast(UIUtils.getString(R.string.tip_conversation_invalid));
            return;
        }

        // 创建消息
        ImageMessage imageMessage = new ImageMessage(file, !useRawImage);

        CubeEngine.getInstance().getMessagingService().sendMessage(conversation, imageMessage,
                new DefaultSendHandler<Conversation, ImageMessage>(true) {
            @Override
            public void handleProcessing(Conversation destination, ImageMessage message) {
                // 将消息添加到界面
                adapter.addLastItem(message);
                moveToBottom();
            }

            @Override
            public void handleProcessed(Conversation destination, ImageMessage message) {
                // 生成缩略图完成
                updateMessageStatus(message);

                UIUtils.postTaskDelay(() -> moveToBottom(), 100);
            }

            @Override
            public void handleSending(Conversation destination, ImageMessage message) {
                updateMessageStatus(message);
            }

            @Override
            public void handleSent(Conversation destination, ImageMessage message) {
                updateMessageStatus(message);
            }
        }, new DefaultFailureHandler(true) {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                updateMessageStatus(imageMessage);
            }
        });
    }

    /**
     * 发送语音消息。
     *
     * @param voiceFile
     * @param duration
     */
    public void sendVoiceMessage(File voiceFile, int duration) {
        if (!this.checkState()) {
            UIUtils.showToast(UIUtils.getString(R.string.tip_conversation_invalid));
            return;
        }

        VoiceMessage voiceMessage = new VoiceMessage(voiceFile, duration);
        adapter.addLastItem(voiceMessage);
        moveToBottom();

        if (!this.messageList.isEmpty()) {
            return;
        }

        CubeEngine.getInstance().getMessagingService().sendMessage(conversation, voiceMessage,
                new DefaultSendHandler<Conversation, VoiceMessage>(true) {
            @Override
            public void handleProcessing(Conversation destination, VoiceMessage message) {
                // 将消息添加到界面
                adapter.addLastItem(message);
                moveToBottom();
            }

            @Override
            public void handleProcessed(Conversation destination, VoiceMessage message) {
                // Nothing
            }

            @Override
            public void handleSending(Conversation destination, VoiceMessage message) {
                updateMessageStatus(message);
            }

            @Override
            public void handleSent(Conversation destination, VoiceMessage message) {
                updateMessageStatus(message);
            }
        }, new DefaultFailureHandler(true) {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                updateMessageStatus(voiceMessage);
            }
        });
    }

    public void moveToBottom() {
        synchronized (this.messageList) {
            if (this.messageList.isEmpty()) {
                return;
            }

            getView().getMessageListView().smoothMoveToPosition(this.messageList.size() - 1);
        }
    }

    private void asyncMarkRead(Message message) {
        Promise.create(new PromiseHandler<Message>() {
            @Override
            public void emit(PromiseFuture<Message> promise) {
                CubeEngine.getInstance().getMessagingService().markRead(message, new DefaultMessageHandler(false) {
                    @Override
                    public void handleMessage(Message message) {
                        promise.resolve(message);
                    }
                }, new DefaultFailureHandler(false) {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {
                        promise.reject(message);
                    }
                });
            }
        }).then(new Future<Message>() {
            @Override
            public void come(Message data) {
                LogUtils.d(TAG, "#asyncMarkRead - success");
            }
        }).catchReject(new Future<Message>() {
            @Override
            public void come(Message data) {
                LogUtils.d(TAG, "#asyncMarkRead - failure");
            }
        }).launch();
    }

    private void updateMessageStatus(Message message) {
        synchronized (this.messageList) {
            List<Message> list = this.messageList;
            for (int i = 0, length = list.size(); i < length; ++i) {
                Message current = list.get(i);
                if (current.id.longValue() == message.id.longValue()) {
                    if (message.getState() == MessageState.Retracted) {
                        // 移除消息
                        adapter.removeItem(i);

                        NotificationMessage notificationMessage =
                                new NotificationMessage(
                                        UIUtils.getString(R.string.tip_message_retracted)
                                                + " " + simpleDateFormat.format(new Date()));

                        CubeEngine.getInstance().getMessagingService().appendMessage(conversation,
                                notificationMessage);

                        adapter.addLastItem(notificationMessage);
                    }
                    else if (message.getState() == MessageState.Deleted) {
                        // 移除消息
                        adapter.removeItem(i);

                        NotificationMessage notificationMessage =
                                new NotificationMessage(
                                        UIUtils.getString(R.string.tip_message_deleted)
                                                + " " + simpleDateFormat.format(new Date()));

                        CubeEngine.getInstance().getMessagingService().appendMessage(conversation,
                                notificationMessage);

                        adapter.addLastItem(notificationMessage);
                    }
                    else {
                        // 修改列表项
                        list.set(i, message);

                        adapter.notifyDataSetChangedWrapper();
                    }
                    break;
                }
            }
        }
    }

    public void switchBurnMode() {
        this.burnMode = !this.burnMode;

        if (this.burnMode) {
            getView().getBurnButtonView().setImageResource(R.mipmap.message_tool_burn_enable);
            UIUtils.showToast(UIUtils.getString(R.string.tip_burn_enabled));
        }
        else {
            getView().getBurnButtonView().setImageResource(R.mipmap.message_tool_burn_disable);
            UIUtils.showToast(UIUtils.getString(R.string.tip_burn_disabled));
        }

        this.conversation.set(AppConsts.BURN_MODE, this.burnMode);
    }

    public void switchVoiceInputMode() {
        this.voiceInputMode = !this.voiceInputMode;

        if (this.voiceInputMode) {
            getView().getVoiceButtonView().setImageResource(R.mipmap.message_tool_keyboard);
            getView().getRecordVoiceButton().setVisibility(View.VISIBLE);
            getView().getInputContentView().setVisibility(View.GONE);
        }
        else {
            getView().getVoiceButtonView().setImageResource(R.mipmap.message_tool_voice);
            getView().getRecordVoiceButton().setVisibility(View.GONE);
            getView().getInputContentView().setVisibility(View.VISIBLE);
        }

        this.conversation.set(AppConsts.VOICE_INPUT_MODE, this.voiceInputMode);
    }

    public void close() throws IOException {
        CubeEngine.getInstance().getMessagingService().removeEventListener(this.conversation, this);
    }

    public void fireItemClick(ViewHolder helper, Message message, int position) {
        if (message instanceof ImageMessage) {
            ImageMessage imageMessage = (ImageMessage) message;
            Intent intent = new Intent(activity, ImageShowcaseActivity.class);
            intent.putExtra("name", imageMessage.getFileName());
            intent.putExtra("messageId", imageMessage.getId());
            if (imageMessage.hasThumbnail()) {
                intent.putExtra("url", imageMessage.getThumbnail().getFileURL());
                intent.putExtra("raw", imageMessage.getFileURL());
            }
            else {
                intent.putExtra("url", imageMessage.getFileURL());
            }
            activity.jumpToActivity(intent);
        }
        else if (message instanceof FileMessage) {
            FileMessage fileMessage = (FileMessage) message;
            if (fileMessage.existsLocal() && !fileMessage.isImageType()) {
                // 尝试打开文件
                if (!FileOpenUtils.openFile(activity, fileMessage.getFilePath())) {
                    UIUtils.showToast(UIUtils.getString(R.string.no_support_file_type));
                }
            }
            else if (fileMessage.isImageType()) {
                // 图像类型文件，使用图片查看器打开
                Intent intent = new Intent(activity, ImageShowcaseActivity.class);
                intent.putExtra("name", fileMessage.getFileName());
                intent.putExtra("url", fileMessage.getFileURL());
                activity.jumpToActivity(intent);
            }
        }
    }

    public void fireItemTouch(View view, MotionEvent motionEvent,
                              ViewHolder helper, Message item, int position) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            long now = System.currentTimeMillis();
            if (now - this.lastTouchTime < 500) {
                // 双击
                if (item instanceof BurnMessage) {
                    this.openBurnMessage(helper, (BurnMessage) item);
                }
            }

            this.lastTouchTime = now;
        }
    }

    public boolean fireItemLongPress(View view, ViewHolder helper, Message message, int position) {
        PopupMenu menu = new PopupMenu(activity, view);
        menu.getMenuInflater().inflate(R.menu.menu_message, menu.getMenu());

        if (message.isSelfTyper()) {
            switch (message.getType()) {
                case Text:
                    break;
                case Image:
                case File:
                    menu.getMenu().getItem(0).setVisible(false);
                    break;
                case Burn:
                    menu.getMenu().getItem(0).setVisible(false);
                    menu.getMenu().getItem(1).setVisible(false);
                    break;
                default:
                    break;
            }
        }
        else {
            // 他人消息不可撤回
            menu.getMenu().getItem(2).setVisible(false);

            switch (message.getType()) {
                case Text:
                    break;
                case Image:
                case File:
                    menu.getMenu().getItem(0).setVisible(false);
                    break;
                case Burn:
                    menu.getMenu().getItem(0).setVisible(false);
                    menu.getMenu().getItem(1).setVisible(false);
                    break;
                default:
                    break;
            }
        }

        // 不是自己的群组不允许双向撤回消息
        if (this.conversation.getType() == ConversationType.Group) {
            if (this.conversation.getGroup().isOwner()) {
                menu.getMenu().getItem(3).setVisible(true);
            }
            else {
                menu.getMenu().getItem(3).setVisible(false);
            }
        }

        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                switch (itemId) {
                    case R.id.menuCopy:
                        copyMessageToClipboard(message);
                        break;
                    case R.id.menuForward:
                        ((MessagePanelActivity) activity).showConversationPickerForForward(message);
                        break;
                    case R.id.menuRetract:
                        retractMessage(message);
                        break;
                    case R.id.menuRetractBoth:
                        retractBothMessage(message);
                        break;
                    case R.id.menuDelete:
                        deleteMessage(message);
                        break;
                    default:
                        break;
                }
                return false;
            }
        });

        menu.show();
        return true;
    }

    private void copyMessageToClipboard(Message message) {
        if (message instanceof HyperTextMessage) {
            HyperTextMessage textMessage = (HyperTextMessage) message;
            ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData data = ClipData.newPlainText("Text",
                    textMessage.getPlaintext());
            cm.setPrimaryClip(data);

            UIUtils.showToast(UIUtils.getString(R.string.message_copied));
        }
    }

    private void retractMessage(Message message) {
        CubeEngine.getInstance().getMessagingService().retractMessage(message,
                new DefaultMessageHandler<Message>(false) {
                    @Override
                    public void handleMessage(Message message) {
                        // Nothing
                    }
                }, new DefaultFailureHandler(true) {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {
                        if (error.code == MessagingServiceState.DataTimeout.code) {
                            UIUtils.showToast(UIUtils.getString(R.string.tip_message_retract_failed_timeout));
                        }
                        else {
                            UIUtils.showToast(UIUtils.getString(R.string.tip_message_retract_failed));
                        }
                    }
                });
    }

    private void retractBothMessage(Message message) {
        CubeEngine.getInstance().getMessagingService().retractBothMessage(message,
                new DefaultMessageHandler<Message>(false) {
                    @Override
                    public void handleMessage(Message message) {
                        // Nothing
                    }
                }, new DefaultFailureHandler(true) {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {
                        UIUtils.showToast(UIUtils.getString(R.string.tip_message_retract_failed));
                    }
                });
    }

    private void deleteMessage(Message message) {
        CubeEngine.getInstance().getMessagingService().deleteMessage(message,
                new DefaultMessageHandler<Message>(false) {
                    @Override
                    public void handleMessage(Message message) {
                        // Nothing
                    }
                }, new DefaultFailureHandler(true) {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {
                        UIUtils.showToast(UIUtils.getString(R.string.tip_message_delete_failed));
                    }
                });
    }

    /**
     * 转发消息。
     *
     * @param message
     * @param target
     * @param addition
     */
    public void forward(Message message, Conversation target, String addition) {
        LogUtils.d(TAG, "#forward - " + message.id + " -> " + target.getDisplayName()
                + " - " + addition);

        // 转发消息
        CubeEngine.getInstance().getMessagingService().forwardMessage(message, target,
                new DefaultMessageHandler<Message>(false) {
                    @Override
                    public void handleMessage(Message message) {
                        CubeApp.getMainThreadHandler().post(() -> {
                            UIUtils.showToast(UIUtils.getString(R.string.tip_forward_success));
                        });

                        // 发送附言
                        if (addition.length() > 0) {
                            HyperTextMessage textMessage = new HyperTextMessage(addition);
                            CubeEngine.getInstance().getMessagingService().sendMessage(target, textMessage,
                                    new SimpleSendHandler(false) {
                                        @Override
                                        public void handleSending(Object destination, Message message) {
                                            // Nothing
                                        }

                                        @Override
                                        public void handleSent(Object destination, Message message) {
                                            // Nothing
                                        }
                                    }, new StableFailureHandler() {
                                        @Override
                                        public void handleFailure(Module module, ModuleError error) {
                                            // Nothing
                                        }
                                    });
                        }
                    }
                }, new DefaultFailureHandler(true) {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {
                        UIUtils.showToast(UIUtils.getString(R.string.tip_forward_failed));
                    }
                });
    }

    private void openBurnMessage(ViewHolder helper, BurnMessage burnMessage) {
        if (burnMessage.hasBurned()) {
            UIUtils.showToast(UIUtils.getString(R.string.message_burned));
            return;
        }

        helper.setViewVisibility(R.id.ivImage, View.GONE);
        helper.setViewVisibility(R.id.llBurnContent, View.VISIBLE);
        helper.setText(R.id.tvText, burnMessage.getContent());
        helper.setText(R.id.tvCountdown, Integer.toString(burnMessage.getReadingTime()));

        CubeEngine.getInstance().getMessagingService().eraseMessageContent(
                burnMessage, burnMessage.getReadingTime(), new DefaultEraseMessageHandler(true) {
                    @Override
                    public void onCountdownStarted(MessagingService service, Message message, int total) {
                        // Nothing
                    }

                    @Override
                    public void onCountdownTick(MessagingService service, Message message, int elapsed, int total) {
                        int countdown = total - elapsed;
                        helper.setText(R.id.tvCountdown, Integer.toString(countdown));
                    }

                    @Override
                    public void onCountdownCompleted(MessagingService service, Message message) {
                        helper.setViewVisibility(R.id.ivImage, View.VISIBLE);
                        helper.setViewVisibility(R.id.llBurnContent, View.GONE);

                        Glide.with(activity).load(R.mipmap.burn_message_read)
                                .override(250, 184)
                                .centerCrop()
                                .into((ImageView) helper.getView(R.id.ivImage));
                    }
                }, new DefaultFailureHandler(true) {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {

                    }
                });
    }

    @Override
    public void onItemClick(ViewHolder helper, ViewGroup parent, View itemView, int position) {
        ((MessagePanelActivity) activity).fireClickBlank();
    }

    @Override
    public void onMessageProcessing(Message message, MessagingService service) {
        // Nothing
    }

    @Override
    public void onMessageProcessed(Message message, MessagingService service) {
        // Nothing
    }

    @Override
    public void onMessageSending(Message message, MessagingService service) {
        // Nothing
    }

    @Override
    public void onMessageSent(Message message, MessagingService service) {
        // Nothing
    }

    @Override
    public void onMessageStated(Message message, MessagingService service) {
        // 消息状态变更
        this.updateMessageStatus(message);
    }

    @Override
    public void onMessageReceived(Message message, MessagingService service) {
        synchronized (this.messageList) {
            this.messageList.add(message);

            Collections.sort(this.messageList, new Comparator<Message>() {
                @Override
                public int compare(Message message1, Message message2) {
                    return (int)(message1.getRemoteTimestamp() - message2.getRemoteTimestamp());
                }
            });
        }

        if (null != this.adapter) {
            this.adapter.notifyDataSetChangedWrapper();
        }

        if (null != getView() && null != getView().getMessageListView()) {
            moveToBottom();

            UIUtils.postTaskDelay(() -> moveToBottom(), 200);
        }

        // 如果当前会话是活跃会话，将接收到的消息标记为已读
        if (null != MessagePanelPresenter.ActiveConversation
            && MessagePanelPresenter.ActiveConversation.equals(this.conversation)) {
            this.asyncMarkRead(message);
        }
    }
}
