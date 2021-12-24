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
 * 多方通信状态码。
 */
public enum MultipointCommState {

    /** 成功。 */
    Ok(0),

    /** 遇到故障。 */
    Failure(9),

    /** 无效域信息。 */
    InvalidDomain(11),

    /** 没有域信息。 */
    NoDomain(12),

    /** 没有设备信息。 */
    NoDevice(13),

    /** 没有找到联系人。 */
    NoContact(14),

    /** 没有找到通讯场域。 */
    NoCommField(15),

    /** 没有找到媒体单元。 */
    NoMediaUnit(16),

    /** 没有找到与媒体单元的数据通道。 */
    NoPipeline(17),

    /** 没有找到 Endpoint 。 */
    NoCommFieldEndpoint(18),

    /** 没有找到对端。 */
    NoPeerEndpoint(19),

    /** 数据结构错误。 */
    DataStructureError(20),

    /** 场域状态错误。 */
    CommFieldStateError(21),

    /** 媒体单元故障。 */
    MediaUnitField(23),

    /** 不被支持的信令。 */
    UnsupportedSignaling(24),

    /** 不支持的操作。 */
    UnsupportedOperation(25),

    /** 正在建立通话。 */
    Calling(30),

    /** 当前线路忙。 */
    Busy(31),

    /** 通话已接通。 */
    CallConnected(33),

    /** 通话结束。 */
    CallBye(35),

    /** 主叫忙。 */
    CallerBusy(41),

    /** 被叫忙。 */
    CalleeBusy(42),

    /** 被主叫阻止。 */
    BeCallerBlocked(45),

    /** 被被叫阻止。 */
    BeCalleeBlocked(46),

    /** 终端未初始化。 */
    Uninitialized(101),

    /** 重复创建连接。 */
    ConnRepeated(103),

    /** 拒绝访问媒体设备。 */
    MediaPermissionDenied(110),

    /** 没有找到视频容器。 */
    NoVideoContainer(111),

    /** 视频容器代理未设置。 */
    VideoContainerAgentNotSetting(112),

    /** 无效的通话记录实例。 */
    InvalidCallRecord(113),

    /** 信令错误。 */
    SignalingError(115),

    /** RTC 节点数据不正确。 */
    RTCPeerError(117),

    /** 创建 RTC offer SDP 错误。 */
    CreateOfferFailed(121),

    /** 创建 RTC answer SDP 错误。 */
    CreateAnswerFailed(122),

    /** 设置本地 SDP 错误。 */
    LocalDescriptionFault(125),

    /** 设置远端 SDP 错误。 */
    RemoteDescriptionFault(126),

    /** 群组状态错误。 */
    GroupStateError(130),

    /** 服务器故障。 */
    ServerFault(200);


    public final int code;

    MultipointCommState(int code) {
        this.code = code;
    }
}
