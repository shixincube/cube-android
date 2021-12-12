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
 * 多方通讯动作描述。
 */
public class MultipointCommAction {

    /**
     * 信令 Offer 。
     */
    public final static String Offer = "offer";

    /**
     * 信令 Answer 。
     */
    public final static String Answer = "answer";

    /**
     * 信令 Bye 。
     */
    public final static String Bye = "bye";

    /**
     * 信令忙 Busy 。
     */
    public final static String Busy = "busy";

    /**
     * ICE 候选字收发。
     */
    public final static String Candidate = "candidate";

    /**
     * 邀请进入场域。
     */
    public final static String Invite = "invite";

    /**
     * 新的终端参与到场域。
     */
    public final static String Arrived = "arrived";

    /**
     * 终端已离开场域。
     */
    public final static String Left = "left";

    /**
     * 获取通讯场域数据。
     */
    public final static String GetField = "getField";

    /**
     * 创建通讯场域。
     */
    public final static String CreateField = "createField";

    /**
     * 销毁通讯场域。
     */
    public final static String DestroyField = "destroyField";

    /**
     * 申请主叫对方。
     */
    public final static String ApplyCall = "applyCall";

    /**
     * 申请加入场域。
     */
    public final static String ApplyJoin = "applyJoin";

    /**
     * 申请终止呼叫。
     */
    public final static String ApplyTerminate = "applyTerminate";

    /**
     * 客户端请求对当期场域进行数据广播。
     */
    public final static String Broadcast = "broadcast";

    private MultipointCommAction() {
    }
}
