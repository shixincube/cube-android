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

package cube.engine.misc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import cube.core.Plugin;
import cube.engine.CubeEngine;
import cube.messaging.Constants;
import cube.messaging.model.Conversation;
import cube.messaging.model.Message;
import cube.util.LogUtils;

/**
 * 消息通知插件。
 */
public class MessageNotifyPlugin implements Plugin<Message> {

    private int requestCode = 101;

    private Context context;

    public MessageNotifyPlugin(Context context) {
        this.context = context;
    }

    @Override
    public Message onEvent(String name, Message data) {
        if (CubeEngine.getInstance().isForeground()) {
            return data;
        }

        Conversation conversation = CubeEngine.getInstance().getMessagingService()
                .getConversationByMessage(data);
        if (null == conversation) {
            return data;
        }

        String title = null;
        String text = null;
        switch (data.getType()) {
            case Text:
            case Image:
            case File:
            case Burn:
                if (data.isSelfTyper()) {
                    return data;
                }

                if (data.isFromGroup()) {
                    title = data.getSourceGroup().getPriorityName();
                }
                else {
                    title = data.getSender().getPriorityName();
                }

                text = data.getSummary();
                break;
            default:
                return data;
        }

        NotificationConfig config = CubeEngine.getInstance().getNotificationConfig();

        Bundle bundle = new Bundle();
        bundle.putLong(Constants.MESSAGE_ID, data.getId());
        bundle.putLong(Constants.CONVERSATION_ID, conversation.getId());

        Intent intent = new Intent();
        intent.putExtra(NotificationConfig.EXTRA_BUNDLE, bundle);

        PendingIntent pendingIntent = null;

        if (null != config.messageNotifyActivityClass) {
            intent.setClass(this.context, config.messageNotifyActivityClass);
            pendingIntent = PendingIntent.getActivity(this.context, this.requestCode, intent,
                    PendingIntent.FLAG_ONE_SHOT);
        }
        else if (null != config.messageNotifyReceiverClass) {
            intent.setClass(this.context, config.messageNotifyReceiverClass);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ComponentName componentName = new ComponentName(this.context.getPackageName(),
                        config.messageNotifyReceiverClass.getName());
                intent.setComponent(componentName);
            }

            pendingIntent = PendingIntent.getBroadcast(this.context, this.requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
        else {
            return data;
        }

        Notification.Builder builder = new Notification.Builder(this.context);
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(title);
        builder.setContentText(text);

        final NotificationManager nm = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(this.context.getPackageName(),
                    "MessageNotifyPlugin", NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(channel);
            builder.setChannelId(this.context.getPackageName());
        }

        if (config.messageNotifySmallIconResource > 0) {
            builder.setSmallIcon(config.messageNotifySmallIconResource);
        }

        LogUtils.d("MessageNotifyPlugin", "Notify message: " + conversation.getDisplayName());

        Handler handler = new Handler(this.context.getMainLooper());
        handler.post(() -> {
            Notification notification = builder.build();
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            nm.notify(data.getId().intValue(), notification);
        });

        return data;
    }
}
