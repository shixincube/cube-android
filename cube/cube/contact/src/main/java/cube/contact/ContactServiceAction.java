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

package cube.contact;

/**
 * 联系人模块动作定义。
 */
public final class ContactServiceAction {

    /**
     * 终端签入。
     */
    public final static String SignIn = "signIn";

    /**
     * 终端签出。
     */
    public final static String SignOut = "signOut";

    /**
     * 恢复终端当前连接。
     */
    public final static String Comeback = "comeback";

    /**
     * 申请当前联系人的所有终端下线。
     */
    public final static String Leave = "leave";

    /**
     * 设备断线。
     */
    public final static String Disconnect = "disconnect";

    /**
     * 设备超时。
     */
    public final static String DeviceTimeout = "deviceTimeout";

    /**
     * 获取联系人信息。
     */
    public final static String GetContact = "getContact";

    /**
     * 修改联系人信息。
     */
    public final static String ModifyContact = "modifyContact";

    /**
     * 按照指定参数列出联系人分区清单。
     */
    public final static String ListContactZones = "listContactZones";

    /**
     * 获取联系人分区。
     */
    public final static String GetContactZone = "getContactZone";

    /**
     * 创建联系人分区。
     */
    public final static String CreateContactZone = "createContactZone";

    /**
     * 删除联系人分区。
     */
    public final static String DeleteContactZone = "deleteContactZone";

    /**
     * 指定分区是否包含指定参与人。
     */
    public final static String ContainsParticipantInZone = "containsParticipantInZone";

    /**
     * 添加参与人到分区。
     */
    public final static String AddParticipantToZone = "addParticipantToZone";

    /**
     * 从分区移除参与人。
     */
    public final static String RemoveParticipantFromZone = "removeParticipantFromZone";

    /**
     * 修改分区参与人信息。
     */
    public final static String ModifyZoneParticipant = "modifyZoneParticipant";

    /**
     * 修改分区信息，有参与人加入或移除。
     */
    public final static String ModifyZone = "modifyZone";

    /**
     * 获取群组信息。
     */
    public final static String GetGroup = "getGroup";

    /**
     * 按照指定参数列出群组清单。
     */
    public final static String ListGroups = "listGroups";

    /**
     * 创建群组。
     */
    public final static String CreateGroup = "createGroup";

    /**
     * 解散群组。
     */
    public final static String DismissGroup = "dismissGroup";

    /**
     * 向群组添加成员。
     */
    public final static String AddGroupMember = "addGroupMember";

    /**
     * 从群组移除成员。
     */
    public final static String RemoveGroupMember = "removeGroupMember";

    /**
     * 修改群组信息。
     */
    public final static String ModifyGroup = "modifyGroup";

    /**
     * 获取指定的附录。
     */
    public final static String GetAppendix = "getAppendix";

    /**
     * 更新附录。
     */
    public final static String UpdateAppendix = "updateAppendix";

    /**
     * 群组的附录已更新。
     */
    public final static String GroupAppendixUpdated = "groupAppendixUpdated";

    /**
     * 阻止清单操作。
     */
    public final static String BlockList = "blockList";

    /**
     * 搜索联系人或群组。
     */
    public final static String Search = "search";

    private ContactServiceAction() {
    }
}
