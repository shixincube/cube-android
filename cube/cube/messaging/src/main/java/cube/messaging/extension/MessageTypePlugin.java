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
import org.json.JSONObject;

import cube.core.Plugin;
import cube.messaging.hook.InstantiateHook;
import cube.messaging.model.Message;

/**
 * 消息分类实例化插件。
 */
public class MessageTypePlugin implements Plugin<Message> {

    public MessageTypePlugin() {
    }

    @Override
    public Message onEvent(String name, Message data) {
        if (InstantiateHook.NAME.equals(name)) {
            return this.onInstantiate(data);
        }

        return data;
    }

    /**
     * 当消息需要实例化（分化）时调用该回调。
     *
     * @param message 原始消息实例。
     * @return 消息实例。
     */
    protected Message onInstantiate(Message message) {
        JSONObject payload = message.getPayload();
        if (payload.has("type")) {
            try {
                String type = payload.getString("type");
                if (MessageTypeName.Hypertext.equals(type)) {
                    return new HyperTextMessage(message);
                }
                else if (MessageTypeName.Image.equals(type)) {
                    return new ImageMessage(message);
                }
                else if (MessageTypeName.File.equals(type)) {
                    return new FileMessage(message);
                }
                else if (MessageTypeName.Text.equals(type)) {
                    return new TextMessage(message);
                }
            } catch (JSONException e) {
                // Nothing
            }
        }

        return message;
    }
}
