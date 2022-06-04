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

import cube.messaging.model.Message;
import cube.messaging.model.MessageType;

/**
 * 阅后即焚消息。
 */
public class BurnMessage extends TypeableMessage {

    public BurnMessage(String text, int readingTime) {
        super(MessageType.Burn);

        try {
            this.payload.put("type", MessageTypeName.Burn);
            this.payload.put("content", text);
            this.payload.put("readingTime", readingTime);
        } catch (JSONException e) {
            // Nothing
        }

        this.summary = text;
    }

    public BurnMessage(Message message) {
        super(message, MessageType.Burn);

        try {
            this.payload.put("type", MessageTypeName.Burn);

            this.summary = this.payload.getString("content");
        } catch (JSONException e) {
            // Nothing
        }
    }

    public String getContent() {
        return this.summary;
    }

    public int getReadingTime() {
        try {
            return this.payload.getInt("readingTime");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return 0;
    }
}
