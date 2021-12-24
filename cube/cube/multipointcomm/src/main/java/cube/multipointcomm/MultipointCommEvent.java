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

package cube.multipointcomm;

/**
 * 多方通讯事件。
 */
public class MultipointCommEvent {

    /**
     * 有新的通话邀请。
     * 事件数据：{@link cube.multipointcomm.model.CallRecord} - 通话记录。
     */
    public final static String NewCall = "NewCall";

    /**
     * 正在处理通话请求。
     * 事件数据：{@link cube.multipointcomm.model.CallRecord} - 发起或应答的通话记录。
     */
    public final static String InProgress = "InProgress";

    /**
     * 对方振铃。
     * 事件数据：{@link cube.multipointcomm.model.CallRecord} - 通话记录。
     */
    public final static String Ringing = "Ringing";

    /**
     * 已经建立连接。
     * 事件数据：{@link cube.multipointcomm.model.CallRecord} - 通话记录。
     */
    public final static String Connected = "Connected";

    /**
     * 对方忙。
     * 事件数据：{@link cube.multipointcomm.model.CallRecord} - 通话记录。
     */
    public final static String Busy = "Busy";

    /**
     * 结束当前通话。
     * 事件数据：{@link cube.multipointcomm.model.CallRecord} - 通话记录。
     */
    public final static String Bye = "Bye";

    /**
     * 呼叫或应答超时。
     * 事件数据：{@link cube.multipointcomm.model.CallRecord} - 通话记录。
     */
    public final static String Timeout = "Timeout";

    /**
     * 已经接收参与者数据。
     * 事件数据：{@link cube.multipointcomm.model.CommFieldEndpoint} - 终端的实例。
     */
    public final static String Followed = "Followed";

    /**
     * 已经停止接收参与者数据。
     * 事件数据：{@link cube.multipointcomm.model.CommFieldEndpoint} - 终端的实例。
     */
    public final static String Unfollowed = "Unfollowed";

    /**
     * 被邀请加入通话。
     * 事件数据：{@link cube.multipointcomm.model.CommField} - 发出邀请的通讯场域的实例。
     */
    public final static String Invited = "Invited";

    /**
     * 新参与者加入。
     * 事件数据：{@link cube.multipointcomm.model.CommFieldEndpoint} - 已加入终端的实例。
     */
    public final static String Arrived = "Arrived";

    /**
     * 参与者已离开。
     * 事件数据：{@link cube.multipointcomm.model.CommFieldEndpoint} - 已离开终端的实例。
     */
    public final static String Left = "Left";

    /**
     * 发生错误。
     * 事件数据：{@link cube.core.ModuleError} - 错误描述。
     */
    public final static String Failed = "Failed";

    private MultipointCommEvent() {
    }
}
