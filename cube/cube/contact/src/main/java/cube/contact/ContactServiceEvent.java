/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Shixin Cube Team.
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

package cube.contact;

/**
 * 联系人事件。
 */
public final class ContactServiceEvent {

    /**
     * 当前客户端的联系人签入。
     */
    public final static String SignIn = "SignIn";

    /**
     * 当前客户端的联系人签出。
     */
    public final static String SignOut = "SignOut";

    /**
     * 当前客户端的联系人恢复连接。
     */
    public final static String Comeback = "Comeback";

    /**
     * 当前客户端的联系人数据就绪。该事件依据网络连通情况和签入账号情况可能被多次触发。
     */
    public final static String SelfReady = "SelfReady";

    /**
     * 群组已更新。
     */
    public final static String GroupUpdated = "GroupUpdated";

    /**
     * 群组被创建。
     */
    public final static String GroupCreated = "GroupCreated";

    /**
     * 群组已解散。
     */
    public final static String GroupDissolved = "GroupDissolved";

    /**
     * 群成员加入。
     */
    public final static String GroupMemberAdded = "GroupMemberAdded";

    /**
     * 群成员移除。
     */
    public final static String GroupMemberRemoved = "GroupMemberRemoved";

    /**
     * 群组的附录进行了实时更新。
     */
    public final static String GroupAppendixUpdated = "GroupAppendixUpdated";

    /**
     * 遇到程序故障。
     */
    public final static String Fault = "Fault";

    /**
     * 未知事件。
     */
    public final static String Unknown = "Unknown";

}
