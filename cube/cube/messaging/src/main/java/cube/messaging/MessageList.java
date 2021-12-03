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

package cube.messaging;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import cube.messaging.model.Message;

/**
 * 消息列表。
 * 用于加速读取消息列表的速度。
 */
public class MessageList implements Comparator<Message> {

    public final List<Message> messages = new Vector<>();

    public boolean hasMore = false;

    private long lifespan = 5L * 60L * 1000L;

    public MessageList() {
    }

    protected void reset(MessageListResult result) {
        hasMore = result.hasMore();

        messages.clear();
        messages.addAll(result.getList());
    }

    protected void extendLife(long lifespan) {
        for (Message message : messages) {
            message.entityLifeExpiry += lifespan;
        }
    }

    protected void appendMessage(Message message) {
        if (this.messages.contains(message)) {
            return;
        }

        this.messages.add(message);

        this.extendLife(this.lifespan);
    }

    protected void insertMessages(List<Message> messageList) {
        for (Message message : messageList) {
            if (this.messages.contains(message)) {
                continue;
            }

            this.messages.add(message);
        }

        // 排序
        Collections.sort(this.messages, this);

        this.extendLife(this.lifespan);
    }

    @Override
    public int compare(Message message1, Message message2) {
        // 时间戳升序
        return (int) (message1.getRemoteTimestamp() - message2.getRemoteTimestamp());
    }
}
