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
 * 消息服务事件。
 */
public final class MessagingServiceEvent {

    /**
     * 当消息模块就绪时。
     */
    public final static String Ready = "Ready";

    /**
     * 收到新消息。
     */
    public final static String Notify = "Notify";

    /**
     * 消息已经发出。
     */
    public final static String Sent = "Sent";

    /**
     * 消息正在发送。
     */
    public final static String Sending = "Sending";

    /**
     * 消息数据处理中。
     */
    public final static String Processing = "Processing";

    /**
     * 消息被撤回。
     */
    public final static String Recall = "Recall";

    /**
     * 消息被删除。
     */
    public final static String Delete = "Delete";

    /**
     * 消息已读。
     */
    public final static String Read = "Read";

    /**
     * 消息被发送到服务器成功标记为仅作用于自己设备。
     */
    public final static String MarkOnlyOwner = "MarkOnlyOwner";

    /**
     * 消息被阻止发送。
     */
    public final static String SendBlocked = "SendBlocked";

    /**
     * 消息被阻止接收。
     */
    public final static String ReceiveBlocked = "ReceiveBlocked";

    /**
     * 消息处理故障。
     */
    public final static String Fault = "Fault";

    /**
     * 未知事件。仅用于调试。
     */
    public final static String Unknown = "Unknown";
}
