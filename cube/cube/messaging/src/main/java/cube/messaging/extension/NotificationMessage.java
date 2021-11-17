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

package cube.messaging.extension;

import org.json.JSONException;

import cube.messaging.model.Message;
import cube.messaging.model.MessageScope;
import cube.messaging.model.MessageType;

/**
 * 通知形式的消息。
 */
public class NotificationMessage extends TypeableMessage {

    public NotificationMessage(String content) {
        super(MessageType.Notification);

        try {
            this.payload.put("type", MessageTypeName.Notification);
            this.payload.put("content", content);
        } catch (JSONException e) {
            // Nothing
        }

        this.scope = MessageScope.Private;
        this.summary = "[通知]";
    }

    public NotificationMessage(Message message) {
        super(message, MessageType.Notification);

        try {
            this.payload.put("type", MessageTypeName.Notification);
        } catch (JSONException e) {
            // Nothing
        }

        this.scope = MessageScope.Private;
        this.summary = "[通知]";
    }

    /**
     * 获取消息内容。
     *
     * @return
     */
    public String getContent() {
        try {
            return this.payload.getString("content");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
