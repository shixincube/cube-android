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

import com.shixincube.app.R;
import com.shixincube.app.ui.activity.ImageShowcaseActivity;
import com.shixincube.app.ui.adapter.MessagePanelAdapter;
import com.shixincube.app.ui.base.BaseFragmentActivity;
import com.shixincube.app.ui.base.BaseFragmentPresenter;
import com.shixincube.app.ui.view.MessagePanelView;
import com.shixincube.app.util.FileOpenUtils;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.adapter.OnItemClickListener;
import com.shixincube.app.widget.adapter.ViewHolder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.CompletionHandler;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;
import cube.messaging.MessageEventListener;
import cube.messaging.MessageListResult;
import cube.messaging.MessagingService;
import cube.messaging.extension.FileMessage;
import cube.messaging.extension.HyperTextMessage;
import cube.messaging.handler.DefaultSendHandler;
import cube.messaging.handler.MessageListResultHandler;
import cube.messaging.model.Conversation;
import cube.messaging.model.Message;
import cube.util.LogUtils;

/**
 * 消息面板。
 */
public class MessagePanelPresenter extends BaseFragmentPresenter<MessagePanelView> implements OnItemClickListener, MessageEventListener {

    private int pageSize = 10;

    private boolean hasMoreMessage = true;

    private MessagePanelAdapter adapter;

    private Conversation conversation;

    public MessagePanelPresenter(BaseFragmentActivity activity, Conversation conversation) {
        super(activity);
        this.conversation = conversation;
        CubeEngine.getInstance().getMessagingService().addEventListener(conversation, this);
    }

    public void markAllRead() {
        CubeEngine.getInstance().getMessagingService().markRead(conversation,
                new CompletionHandler() {
                    @Override
                    public void handleCompletion(Module module) {
                        // Nothing
                    }
                });
    }

    public void loadMessages() {
        MessageListResult result = CubeEngine.getInstance().getMessagingService().getRecentMessages(this.conversation, this.pageSize);
        List<Message> messageList = result.getList();
        this.hasMoreMessage = result.hasMore();

        if (null == this.adapter) {
            this.adapter = new MessagePanelAdapter(activity, messageList, this);
            this.adapter.setOnItemClickListener(this);

            getView().getMessageListView().setAdapter(this.adapter);

            UIUtils.postTaskDelay(() -> moveToBottom(), 200);
        }
        else {
            this.adapter.notifyDataSetChangedWrapper();
            moveToBottom();
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

        List<Message> messages = this.adapter.getData();
        Message message = null;
        if (!messages.isEmpty()) {
            message = messages.get(0);
        }

        int max = this.pageSize + this.pageSize;
        CubeEngine.getInstance().getMessagingService().queryMessages(conversation, message, max,
                new MessageListResultHandler() {
                    @Override
                    public void handle(List<Message> messageList, boolean hasMore) {
                        // 结束刷新
                        getView().getRefreshLayout().endRefreshing();

                        hasMoreMessage = hasMore;

                        if (messageList.isEmpty()) {
                            return;
                        }

                        // Result 清单里的消息是从旧到新的，时间正序，因此倒着插入到列表
                        for (int i = messageList.size() - 1; i >= 0; --i) {
                            messages.add(0, messageList.get(i));
                        }

                        getView().getMessageListView().moveToPosition(messageList.size() - 1);
                    }
                });
    }

    public void sendTextMessage() {
        String text = getView().getInputContentView().getText().toString().trim();
        this.sendTextMessage(text);
        getView().getInputContentView().setText("");
    }

    public void sendTextMessage(String text) {
        HyperTextMessage textMessage = new HyperTextMessage(text);
        CubeEngine.getInstance().getMessagingService().sendMessage(conversation, textMessage,
                new DefaultSendHandler<Conversation, HyperTextMessage>(true) {
                    @Override
                    public void handleProcessing(Conversation destination, HyperTextMessage message) {
                        // 将消息添加到界面
                        adapter.addLastItem(message);
                        moveToBottom();
                    }

                    @Override
                    public void handleSending(Conversation destination, HyperTextMessage message) {
                        // Nothing
                    }

                    @Override
                    public void handleSent(Conversation destination, HyperTextMessage message) {
                        // Nothing
                    }
                }, new DefaultFailureHandler(true) {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {
                        // 消息错误，更新显示状态

                    }
                });
    }

    public void sendFileMessage(File rawFile) {
        FileMessage fileMessage = new FileMessage(rawFile);
        CubeEngine.getInstance().getMessagingService().sendMessage(conversation, fileMessage,
                new DefaultSendHandler<Conversation, FileMessage>(true) {
                @Override
                public void handleProcessing(Conversation destination, FileMessage message) {
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
                }
            }, new DefaultFailureHandler(true) {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    LogUtils.i(this.getClass().getSimpleName(), "#sendFileMessage - handleFailure : " + error.code);
                }
            });
    }

    private void moveToBottom() {
        getView().getMessageListView().smoothMoveToPosition(this.adapter.getData().size() - 1);
    }

    @Override
    public void close() throws IOException {
        CubeEngine.getInstance().getMessagingService().removeEventListener(this.conversation, this);
    }

    @Override
    public void onItemClick(ViewHolder helper, ViewGroup parent, View itemView, int position) {
        Message message = this.adapter.getData().get(position);
        if (message instanceof FileMessage) {
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
    public void onMessageSending(Message message, MessagingService service) {
        // Nothing
    }

    @Override
    public void onMessageSent(Message message, MessagingService service) {
        // Nothing
    }

    @Override
    public void onMessageReceived(Message message, MessagingService service) {
        List<Message> list = this.adapter.getData();
        list.add(message);

        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChangedWrapper();
                if (null != getView() && null != getView().getMessageListView()) {
                    moveToBottom();
                }
            }
        });
    }
}
