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
 * 仅包含单个文件的消息。
 */
public class FileMessage extends TypeableMessage {

    public FileMessage(File file) {
        super(MessageType.File);

        // 创建消息附件
        FileAttachment attachment = new FileAttachment(file);
        this.setAttachment(attachment);

        try {
            this.payload.put("type", MessageTypeName.File);
        } catch (JSONException e) {
            // Nothing
        }

        this.summary = "[文件] " + file.getName();
    }

    public FileMessage(Message message) {
        super(message, MessageType.File);

        try {
            if (!this.payload.has("type")) {
                this.payload.put("type", MessageTypeName.File);
            }
        } catch (JSONException e) {
            // Nothing
        }

        this.summary = "[文件] " + message.getAttachment().getFileName();
    }

    /**
     * 获取文件名。
     *
     * @return
     */
    public String getFileName() {
        return this.getAttachment().getFileName();
    }

    /**
     * 获取文件大小。
     *
     * @return
     */
    public long getFileSize() {
        return this.getAttachment().getFileSize();
    }

    /**
     * 获取文件类型。
     *
     * @return
     */
    public String getFileType() {
        return this.getAttachment().getFileType();
    }

    /**
     * 获取文件最近一次修改时间戳。
     *
     * @return
     */
    public long getFileLastModified() {
        return this.getAttachment().getFileLastModified();
    }

    /**
     * 获取当前文件路径。
     *
     * @return
     */
    public String getFilePath() {
        return (null != this.getAttachment().getFile()) ?
                this.getAttachment().getFile().getPath() : null;
    }

    /**
     * 文件是否在本地存在。
     *
     * @return
     */
    public boolean existsLocal() {
        return this.getAttachment().existsLocal();
    }

    /**
     * 是否是图像类型文件。
     *
     * @return
     */
    public boolean isImageType() {
        return this.getAttachment().isImageType();
    }

    /**
     * 获取文件已经被处理的数据大小。
     * 该方法仅在文件消息被处理时有效。
     *
     * @return
     */
    public long getProcessedSize() {
        return this.getAttachment().getProcessedSize();
    }
}
