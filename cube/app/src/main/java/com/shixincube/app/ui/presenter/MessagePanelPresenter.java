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

import com.shixincube.app.ui.adapter.MessagePanelAdapter;
import com.shixincube.app.ui.base.BaseFragmentActivity;
import com.shixincube.app.ui.base.BaseFragmentPresenter;
import com.shixincube.app.ui.view.MessagePanelView;
import com.shixincube.app.util.UIUtils;

import java.io.IOException;
import java.util.List;

import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.CompletionHandler;
import cube.core.handler.FailureHandler;
import cube.engine.CubeEngine;
import cube.messaging.MessageEventListener;
import cube.messaging.MessageListResult;
import cube.messaging.MessagingService;
import cube.messaging.extension.HyperTextMessage;
import cube.messaging.handler.SendHandler;
import cube.messaging.model.Conversation;
import cube.messaging.model.Message;

/**
 * 消息面板。
 */
public class MessagePanelPresenter extends BaseFragmentPresenter<MessagePanelView> implements MessageEventListener {

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
        MessageListResult result = CubeEngine.getInstance().getMessagingService().getRecentMessages(this.conversation, 20);
        List<Message> messageList = result.getList();

        if (null == this.adapter) {
            this.adapter = new MessagePanelAdapter(activity, messageList, this);
            getView().getMessageListView().setAdapter(this.adapter);

            UIUtils.postTaskDelay(() -> moveToBottom(), 200);
        }
        else {
            this.adapter.notifyDataSetChangedWrapper();
            moveToBottom();
        }
    }

    public void sendTextMessage() {
        String text = getView().getInputContentView().getText().toString().trim();
        this.sendTextMessage(text);
        getView().getInputContentView().setText("");
    }

    public void sendTextMessage(String text) {
        HyperTextMessage textMessage = new HyperTextMessage(text);
        CubeEngine.getInstance().getMessagingService().sendMessage(conversation, textMessage,
                new SendHandler<Conversation>() {
                    @Override
                    public void handleSending(Conversation destination, Message message) {
                        // 将消息添加到界面
                        adapter.addLastItem(message);
                        moveToBottom();
                    }

                    @Override
                    public void handleSent(Conversation destination, Message message) {

                    }
                }, new FailureHandler() {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {
                        // 消息错误，更新显示状态

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
