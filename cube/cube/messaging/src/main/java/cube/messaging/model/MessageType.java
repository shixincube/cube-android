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

package cube.messaging.model;

/**
 * 消息类型。
 */
public enum MessageType {

    /**
     * 文字。
     */
    Text,

    /**
     * 阅后即焚。
     */
    Burn,

    /**
     * 文件。
     */
    File,

    /**
     * 图片。
     */
    Image,

    /**
     * 语音。
     */
    Voice,

    /**
     * 视频。
     */
    Video,

    /**
     * 超链接。
     */
    URL,

    /**
     * 定位。
     */
    Location,

    /**
     * 卡片。
     */
    Card,

    /**
     * 通知。
     */
    Notification,

    /**
     * 系统。
     */
    System,

    /**
     * 空白。
     */
    Blank,

    /**
     * 其他。
     */
    Other,

    /**
     * 未知。
     */
    Unknown
}
