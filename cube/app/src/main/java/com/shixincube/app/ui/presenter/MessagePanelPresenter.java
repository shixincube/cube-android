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

import com.shixincube.app.AppConsts;
import com.shixincube.app.R;
import com.shixincube.app.model.MessageConversation;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;
import cube.engine.util.Future;
import cube.engine.util.Promise;
import cube.engine.util.PromiseFuture;
import cube.engine.util.PromiseHandler;
import cube.messaging.MessageEventListener;
import cube.messaging.MessageListResult;
import cube.messaging.MessagingService;
import cube.messaging.extension.FileMessage;
import cube.messaging.extension.HyperTextMessage;
import cube.messaging.extension.ImageMessage;
import cube.messaging.extension.NotificationMessage;
import cube.messaging.handler.DefaultConversationHandler;
import cube.messaging.handler.DefaultMessageHandler;
import cube.messaging.handler.DefaultMessageListResultHandler;
import cube.messaging.handler.DefaultSendHandler;
import cube.messaging.handler.SimpleSendHandler;
import cube.messaging.model.Conversation;
import cube.messaging.model.ConversationState;
import cube.messaging.model.ConversationType;
import cube.messaging.model.Message;
import cube.util.LogUtils;

/**
 * 消息面板。
 */
public class  MessagePanelPresenter extends BasePresenter<MessagePanelView> implements OnItemClickListener, MessageEventListener {

    private int pageSize = 10;

    private boolean hasMoreMessage = true;

    private MessagePanelAdapter adapter;

    private Conversation conversation;

    private List<Message> messageList;

    private boolean burnMode;

    public MessagePanelPresenter(BaseActivity activity, Conversation conversation) {
        super(activity);
        this.conversation = conversation;
        this.messageList = new ArrayList<>();
        this.burnMode = false;
        CubeEngine.getInstance().getMessagingService().addEventListener(conversation, this);
    }

    public boolean isBurnMode() {
        return this.burnMode;
    }

    public void refreshState() {
        JSONObject context = this.conversation.getContext();
        if (null != context) {
            try {
                this.burnMode = context.has(AppConsts.BURN_MODE)
                        && context.getBoolean(AppConsts.BURN_MODE);

                if (this.burnMode) {
                    getView().getBurnButtonView().setImageResource(R.mipmap.message_tool_burn_enable);
                }
                else {
                    getView().getBurnButtonView().setImageResource(R.mipmap.message_tool_burn_disable);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
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

            UIUtils.postTaskDelay(() -> moveToBottom(), 500);
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
                        LogUtils.d(this.getClass().getSimpleName(), "#sendFileMessage - handleSending : " +
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
                    LogUtils.i(this.getClass().getSimpleName(), "#sendFileMessage - handleFailure : " + error.code);

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

        CubeEngine.getInstance().getMessagingService().sendMessage(conversation, imageMessage, new DefaultSendHandler<Conversation, ImageMessage>(true) {
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

    public void moveToBottom() {
        if (this.messageList.isEmpty()) {
            return;
        }

        getView().getMessageListView().smoothMoveToPosition(this.messageList.size() - 1);
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
                LogUtils.d(AppConsts.TAG, "#asyncMarkRead - success");
            }
        }).catchReject(new Future<Message>() {
            @Override
            public void come(Message data) {
                LogUtils.d(AppConsts.TAG, "#asyncMarkRead - failure");
            }
        }).launch();
    }

    private void updateMessageStatus(Message message) {
        synchronized (this.messageList) {
            List<Message> list = this.messageList;
            for (int i = 0, length = list.size(); i < length; ++i) {
                Message current = list.get(i);
                if (current.id.longValue() == message.id.longValue()) {
                    list.remove(i);
                    list.add(i, message);
                    adapter.notifyDataSetChangedWrapper();
                    break;
                }
            }
        }
    }

    public boolean switchBurnMode() {
        return false;
    }

    public void close() throws IOException {
        CubeEngine.getInstance().getMessagingService().removeEventListener(this.conversation, this);
    }

    public void fireItemClick(ViewHolder helper, Message message, int position) {
        if (message instanceof  ImageMessage) {
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

    @Override
    public void onItemClick(ViewHolder helper, ViewGroup parent, View itemView, int position) {
//        Message message = this.messageList.get(position);
//        this.fireItemClick(helper, message, position);
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
    public void onMessageRead(Message message, MessagingService service) {
        // 消息修改为已读
        this.updateMessageStatus(message);
    }

    @Override
    public void onMessageReceived(Message message, MessagingService service) {
        synchronized (this.messageList) {
            this.messageList.add(message);
        }

        if (null != this.adapter) {
            this.adapter.notifyDataSetChangedWrapper();
        }

        if (null != getView() && null != getView().getMessageListView()) {
            moveToBottom();
        }

        // 如果当前会话是活跃会话，将接收到的消息标记为已读
        if (null != MessageConversation.ActiveConversation
            && MessageConversation.ActiveConversation.equals(this.conversation)) {
            this.asyncMarkRead(message);
        }
    }
}
