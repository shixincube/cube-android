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

package com.shixincube.app.ui.adapter;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.shixincube.app.R;
import com.shixincube.app.ui.presenter.MessagePanelPresenter;
import com.shixincube.app.util.AvatarUtils;
import com.shixincube.app.util.DateUtils;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.BubbleImageView;
import com.shixincube.app.widget.adapter.AdapterForRecyclerView;
import com.shixincube.app.widget.adapter.ViewHolderForRecyclerView;

import java.util.List;

import cube.fileprocessor.util.CalculationUtils;
import cube.messaging.extension.FileMessage;
import cube.messaging.extension.HyperTextMessage;
import cube.messaging.extension.ImageMessage;
import cube.messaging.extension.NotificationMessage;
import cube.messaging.model.Message;
import cube.messaging.model.MessageState;

/**
 * 消息面板数据适配器。
 */
public class MessagePanelAdapter extends AdapterForRecyclerView<Message> {

    private final long showTimeInterval = 10 * 60 * 1000;

    private MessagePanelPresenter presenter;

    public MessagePanelAdapter(Context context, List<Message> data, MessagePanelPresenter presenter) {
        super(context, data);
        this.presenter = presenter;
    }

    @Override
    public void convert(ViewHolderForRecyclerView helper, Message item, int position) {
        this.setView(helper, item, position);
        this.bindEvent(helper, item, position);
    }

    @Override
    public int getItemViewType(int position) {
        Message message = this.getData().get(position);
        if (message instanceof HyperTextMessage) {
            return message.isSelfTyper() ? R.layout.item_message_text_send : R.layout.item_message_text_receive;
        }
        else if (message instanceof ImageMessage) {
            return message.isSelfTyper() ? R.layout.item_message_image_send : R.layout.item_message_image_receive;
        }
        else if (message instanceof FileMessage) {
            return message.isSelfTyper() ? R.layout.item_message_item_send : R.layout.item_message_item_receive;
        }
        else if (message instanceof NotificationMessage) {
            return R.layout.item_message_notification;
        }

        return R.layout.item_message_no_support;
    }

    private void setView(ViewHolderForRecyclerView helper, Message item, int position) {
        setTime(helper, item, position);
        setAvatar(helper, item, position);
        setName(helper, item, position);
        setState(helper, item, position);
        setContent(helper, item, position);
    }

    private void setTime(ViewHolderForRecyclerView helper, Message item, int position) {
        if (position > 0) {
            // 判断是否显示时间
            Message preMessage = getData().get(position - 1);
            if (item.getRemoteTimestamp() - preMessage.getRemoteTimestamp() > this.showTimeInterval) {
                helper.setViewVisibility(R.id.tvTime, View.VISIBLE)
                        .setText(R.id.tvTime, DateUtils.formatConversationTime(item.getRemoteTimestamp()));
            }
            else {
                helper.setViewVisibility(R.id.tvTime, View.GONE);
            }
        }
        else {
            helper.setViewVisibility(R.id.tvTime, View.VISIBLE)
                    .setText(R.id.tvTime, DateUtils.formatConversationTime(item.getRemoteTimestamp()));
        }
    }

    private void setAvatar(ViewHolderForRecyclerView helper, Message item, int position) {
        ImageView view = helper.getView(R.id.ivAvatar);
        if (null == view) {
            return;
        }

        Glide.with(getContext()).load(AvatarUtils.getAvatarResource(item.getSender())).centerCrop().into(view);
    }

    private void setName(ViewHolderForRecyclerView helper, Message item, int position) {
        TextView textView = helper.getView(R.id.tvName);
        if (null == textView) {
            return;
        }

        if (item.isFromGroup()) {
            if (item.getSourceGroup().getAppendix().isDisplayMemberName()) {
                textView.setText(item.getSender().getPriorityName());
                textView.setVisibility(View.VISIBLE);
            }
            else {
                textView.setVisibility(View.GONE);
            }
        }
        else {
            textView.setVisibility(View.GONE);
        }
    }

    private void setState(ViewHolderForRecyclerView helper, Message item, int position) {
        if (item.isFromGroup()) {
            // 群组消息没有已读和未读状态
            return;
        }

        if (item.isSelfTyper()) {
            if (item.getState() == MessageState.Read) {
                helper.setViewVisibility(R.id.llRead, View.VISIBLE);
                helper.setText(R.id.tvRead, UIUtils.getString(R.string.message_read));
            }
            else if (item.getState() == MessageState.Sent) {
                helper.setViewVisibility(R.id.llRead, View.VISIBLE);
                helper.setText(R.id.tvRead, UIUtils.getString(R.string.message_unread));
            }
            else {
                helper.setViewVisibility(R.id.llRead, View.GONE);
            }
        }
    }

    private void setContent(ViewHolderForRecyclerView helper, Message item, int position) {
        if (item instanceof HyperTextMessage) {
            // TODO 提取表情
            HyperTextMessage message = (HyperTextMessage) item;
            helper.setText(R.id.tvText, message.getPlaintext());
        }
        else if (item instanceof ImageMessage) {
            ImageMessage message = (ImageMessage) item;
            BubbleImageView imageView = helper.getView(R.id.bivImage);
            // 加载图片
            Glide.with(getContext()).load(message.hasThumbnail() ? message.getThumbnail().getFileURL() : message.getFileURL())
                    .error(R.mipmap.default_img_failed)
                    .override(UIUtils.dp2px(80), UIUtils.dp2px(150))
                    .centerCrop()
                    .into(imageView);
            UIUtils.postTaskDelay(() -> presenter.moveToBottom(), 100);

            if (message.getState() == MessageState.Sending) {
                imageView.setPercent(message.getProgressPercent());
            }
            else {
                imageView.setProgressVisible(false);
                imageView.showShadow(false);
            }
        }
        else if (item instanceof FileMessage) {
            FileMessage message = (FileMessage) item;
            helper.setText(R.id.tvTitle, message.getFileName());
            helper.setText(R.id.tvDescription, CalculationUtils.formatByteDataSize(message.getFileSize()));
            helper.setImageResource(R.id.ivThumb, UIUtils.getFileIcon(message.getFileType()));
            // 显示正在发送进度
            if (message.isSelfTyper()) {
                if (message.getState() == MessageState.Sending) {
                    helper.setViewVisibility(R.id.pbSending, View.VISIBLE);
                }
                else {
                    helper.setViewVisibility(R.id.pbSending, View.GONE);
                }
            }
        }
        else if (item instanceof NotificationMessage) {
            NotificationMessage message = (NotificationMessage) item;
            helper.setText(R.id.tvNotification, message.getContent());
        }
        else {
            // TODO
        }
    }

    private void bindEvent(ViewHolderForRecyclerView helper, Message item, int position) {
        if (item instanceof ImageMessage) {
            BubbleImageView imageView = helper.getView(R.id.bivImage);
            imageView.setOnClickListener((view) -> this.presenter.fireItemClick(helper, item, position));
        }
        else if (item instanceof FileMessage) {
            LinearLayout layout = helper.getView(R.id.llContact);
            layout.setOnClickListener((view) -> this.presenter.fireItemClick(helper, item, position));
        }
    }
}
