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

import java.util.List;

import cube.messaging.model.Conversation;

/**
 * 消息模块最近事件监听器。
 */
public interface ConversationEventListener {

    /**
     * 当有相关会话的消息更新时该方法被回调。
     *
     * @param conversation 有消息更新的会话。
     * @param service 消息服务。
     */
    void onConversationMessageUpdated(Conversation conversation, MessagingService service);

    /**
     * 当有相关会话更新时该方法被回调。
     *
     * @param conversation 被更新的会话。
     * @param service 消息服务。
     */
    void onConversationUpdated(Conversation conversation, MessagingService service);

    /**
     * 当会话清单更新时该方法被回调。
     *
     * @param conversationList 被更新的会话清单。
     * @param service 消息服务。
     */
    void onConversationListUpdated(List<Conversation> conversationList, MessagingService service);
}
