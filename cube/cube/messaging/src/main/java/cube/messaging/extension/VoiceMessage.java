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

import org.json.JSONException;

import java.io.File;

import cube.messaging.model.FileAttachment;
import cube.messaging.model.Message;
import cube.messaging.model.MessageType;

/**
 * 语音消息。
 */
public class VoiceMessage extends TypeableMessage {

    public VoiceMessage(File file, int duration) {
        super(MessageType.Voice);
        try {
            this.payload.put("type", MessageTypeName.Voice);
            this.payload.put("duration", duration);
        } catch (JSONException e) {
            // Nothing
        }

        this.summary = "[语音]";

        // 创建消息附件
        FileAttachment attachment = new FileAttachment(file);
        this.setAttachment(attachment);
    }

    public VoiceMessage(Message message) {
        super(message, MessageType.Voice);
        try {
            this.payload.put("type", MessageTypeName.Voice);
        } catch (JSONException e) {
            // Nothing
        }

        this.summary = "[语音]";
    }

    /**
     * 获取语音时长。
     *
     * @return
     */
    public int getDuration() {
        try {
            return this.payload.getInt("duration");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
