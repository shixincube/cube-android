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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import cube.core.Plugin;
import cube.engine.CubeEngine;
import cube.messaging.model.Message;

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
        NotificationConfig config = CubeEngine.getInstance().getNotificationConfig();

        NotificationManager nm = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent();
        intent.putExtra("messageId", data.getId());
        try {
            Class<?> activityClass = ClassLoader.getSystemClassLoader().loadClass(config.messageNotifyActivityClassName);
            intent.setClass(context, activityClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this.context, requestCode, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Notification.Builder builder = new Notification.Builder(this.context);
//        builder.setSmallIcon(R.mipmap.ic_launcher);
        Notification notification = builder.build();

        return data;
    }
}
