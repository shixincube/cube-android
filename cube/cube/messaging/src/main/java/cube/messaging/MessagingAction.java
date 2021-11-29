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

/**
 * 消息模块动作。
 */
public final class MessagingAction {

    /**
     * 向服务器发送消息。
     */
    public final static String Push = "push";

    /**
     * 从服务器拉取消息。
     */
    public final static String Pull = "pull";

    /**
     * 收到在线消息。
     */
    public final static String Notify = "notify";

    /**
     * 撤回消息。
     */
    public final static String Recall = "recall";

    /**
     * 删除消息。
     */
    public final static String Delete = "delete";

    /**
     * 标记已读。
     */
    public final static String Read = "read";

    /**
     * 查询消息状态信息。
     */
    public final static String QueryState = "queryState";

    /**
     * 获取会话列表。
     */
    public final static String GetConversations = "getConversations";

    /**
     * 更新会话数据。
     */
    public final static String UpdateConversation = "updateConversation";
}
