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

package cube.messaging.extension;

/**
 * 消息类型。
 */
public final class MessageTypeName {

    /**
     * 一般文本类型。
     */
    public final static String Text = "text";

    /**
     * 阅后即焚类型。
     */
    public final static String Burn = "burn";

    /**
     * 超文本消息类型。
     */
    public final static String Hypertext = "hypertext";

    /**
     * 文件消息。
     */
    public final static String File = "file";

    /**
     * 图片消息。
     */
    public final static String Image = "image";

    /**
     * 语音消息。
     */
    public final static String Voice = "voice";

    /**
     * 通知消息。
     */
    public final static String Notification = "notification";

    /**
     * 空白消息。
     */
    public final static String Blank = "blank";

    private MessageTypeName() {
    }
}
