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

import cube.messaging.model.Message;

/**
 * 消息事件监听器。
 */
public interface MessageEventListener {

    /**
     * 消息正在处理附件数据。
     *
     * @param message 消息实体。
     * @param service 消息服务。
     */
    void onMessageProcessing(Message message, MessagingService service);

    /**
     * 消息附件数据已处理完成。
     *
     * @param message 消息实体。
     * @param service 消息服务。
     */
    void onMessageProcessed(Message message, MessagingService service);

    /**
     * 消息正在发送。
     *
     * @param message 消息实体。
     * @param service 消息服务。
     */
    void onMessageSending(Message message, MessagingService service);

    /**
     * 消息已经发送。
     *
     * @param message 消息实体。
     * @param service 消息服务。
     */
    void onMessageSent(Message message, MessagingService service);

    /**
     * 消息逻辑状态改变。
     *
     * @param message 消息实体。
     * @param service 消息服务。
     */
    void onMessageStated(Message message, MessagingService service);

    /**
     * 接收到新消息。
     *
     * @param message 消息实体。
     * @param service 消息服务。
     */
    void onMessageReceived(Message message, MessagingService service);
}
