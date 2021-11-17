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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cell.util.collection.FlexibleByteBuffer;
import cube.messaging.model.Message;
import cube.messaging.model.MessageType;

/**
 * 超文本消息。
 */
public class HyperTextMessage extends TypeableMessage {

    private final static String FORMAT_KEY_TEXT = "text";
    private final static String FORMAT_KEY_DESC = "desc";
    private final static String FORMAT_KEY_NAME = "name";
    private final static String FORMAT_KEY_CODE = "code";
    private final static String FORMAT_KEY_ID = "id";

    private List<FormattedContent> formattedContents = new ArrayList<>();

    private String plaintext;

    /**
     * 构造函数。
     *
     * @param text 指定文本。
     */
    public HyperTextMessage(String text) {
        super(MessageType.Text);
        try {
            this.payload.put("type", MessageTypeName.Hypertext);
            this.payload.put("content", text);
        } catch (JSONException e) {
            // Nothing
        }

        this.plaintext = text;

        // 解析数据
        this.parse(text);
    }

    public HyperTextMessage(Message message) {
        super(message, MessageType.Text);

        try {
            this.payload.put("type", MessageTypeName.Hypertext);

            this.plaintext = this.payload.getString("content");
        } catch (JSONException e) {
            // Nothing
        }

        // 解析数据
        this.parse(this.plaintext);
    }

    public String getPlaintext() {
        return this.plaintext;
    }

    public List<FormattedContent> getFormattedContents() {
        return new ArrayList<>(this.formattedContents);
    }

    private void parse(String text) {
        // AT Format: [@ name # id ]
        // Emoji Format: [E desc # code ]

        boolean phaseEmoji = false;
        boolean phaseAt = false;
        FlexibleByteBuffer content = new FlexibleByteBuffer(128);
        FlexibleByteBuffer format = new FlexibleByteBuffer(32);

        byte[] input = text.getBytes(StandardCharsets.UTF_8);

        for (int i = 0, length = input.length; i < length; ++i) {
            byte c = input[i];
            if (c == '\\') {
                // 转义
                c = input[++i];
                if (!phaseEmoji && !phaseAt) {
                    content.put(c);
                }
                else {
                    format.put(c);
                }
            }
            else if (c == '[') {
                byte next = input[i + 1];
                if (next == 'E') {
                    // 记录之前缓存里的文本数据
                    content.flip();
                    String data = toString(content);
                    FormattedContent fc = new FormattedContent(FormattedContentFormat.Text);
                    fc.content.put(FORMAT_KEY_TEXT, data);
                    formattedContents.add(fc);
                    content.clear();

                    phaseEmoji = true;
                    ++i;    // 跳过 next
                }
                else if (next == '@') {
                    // 记录之前缓存里的文本数据
                    content.flip();
                    String data = toString(content);
                    FormattedContent fc = new FormattedContent(FormattedContentFormat.Text);
                    fc.content.put(FORMAT_KEY_TEXT, data);
                    this.formattedContents.add(fc);
                    content.clear();

                    phaseAt = true;
                    ++i;    // 跳过 next
                }
            }
            else if (c == ']') {
                if (phaseEmoji) {
                    phaseEmoji = false;
                    format.flip();
                    String data = toString(format);
                    Map<String, String> emojiResult = parseEmoji(data);
                    format.clear();

                    FormattedContent fc = new FormattedContent(FormattedContentFormat.Emoji, emojiResult);
                    this.formattedContents.add(fc);
                }
                else if (phaseAt) {
                    phaseAt = false;
                    format.flip();
                    String data = toString(format);
                    Map<String, String> atResult = parseAt(data);
                    format.clear();

                    FormattedContent fc = new FormattedContent(FormattedContentFormat.At, atResult);
                    this.formattedContents.add(fc);
                }
            }
            else {
                if (!phaseEmoji && !phaseAt) {
                    content.put(c);
                }
                else {
                    format.put(c);
                }
            }
        }

        if (content.position() > 0) {
            content.flip();
            String data = toString(content);
            FormattedContent fc = new FormattedContent(FormattedContentFormat.Text);
            fc.content.put(FORMAT_KEY_TEXT, data);
            this.formattedContents.add(fc);
        }
        content.clear();

        // 生成平滑文本
        StringBuilder plain = new StringBuilder();
        for (FormattedContent fc : this.formattedContents) {
            if (fc.format == FormattedContentFormat.Text) {
                plain.append(fc.getText());
            }
            else if (fc.format == FormattedContentFormat.Emoji) {
                plain.append("[");
                plain.append(fc.getDesc());
                plain.append("]");
            }
            else if (fc.format == FormattedContentFormat.At) {
                plain.append(" @");
                plain.append(fc.getName());
                plain.append(" ");
            }
        }

        this.plaintext = plain.toString();
        this.summary = this.plaintext;
        plain = null;
    }

    private Map<String, String> parseEmoji(String input) {
        // Format: [ desc # code ]
        int index = input.indexOf("#");
        Map<String, String> result = new HashMap<>();
        result.put(FORMAT_KEY_DESC, input.substring(0, index));
        result.put(FORMAT_KEY_CODE, input.substring(index + 1));
        return result;
    }

    private Map<String, String> parseAt(String input) {
        // Format: [ name # id ]
        int index = input.indexOf("#");
        Map<String, String> result = new HashMap<>();
        result.put(FORMAT_KEY_NAME, input.substring(0, index));
        result.put(FORMAT_KEY_ID, input.substring(index + 1));
        return result;
    }

    private String toString(FlexibleByteBuffer buffer) {
        return new String(buffer.array(), 0, buffer.limit(), StandardCharsets.UTF_8);
    }

    /**
     * 内容格式描述。
     */
    public enum FormattedContentFormat {
        /**
         * 一般文本格式。
         */
        Text,

        /**
         * 提醒联系人格式。
         */
        At,

        /**
         * 表情符号格式。
         */
        Emoji,

        /**
         * 未知格式。
         */
        Unknown
    }

    /**
     * 格式化的内容。
     */
    protected class FormattedContent {

        protected FormattedContentFormat format;

        protected Map<String, String> content;

        protected FormattedContent(FormattedContentFormat format) {
            this.format = format;
            this.content = new HashMap<>();
        }

        protected FormattedContent(FormattedContentFormat format, Map<String, String> content) {
            this.format = format;
            this.content = content;
        }

        public String getText() {
            return this.content.get(FORMAT_KEY_TEXT);
        }

        public String getDesc() {
            return this.content.get(FORMAT_KEY_DESC);
        }

        public String getName() {
            return this.content.get(FORMAT_KEY_NAME);
        }

        public String getCode() {
            return this.content.get(FORMAT_KEY_CODE);
        }

        public String getID() {
            return this.content.get(FORMAT_KEY_ID);
        }
    }
}
