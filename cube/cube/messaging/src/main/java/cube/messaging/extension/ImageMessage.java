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

import java.io.File;

import cube.fileprocessor.model.FileThumbnail;
import cube.filestorage.model.FileLabel;
import cube.messaging.model.FileAttachment;
import cube.messaging.model.Message;
import cube.messaging.model.MessageType;

/**
 * 仅包含单个图像文件的消息。
 */
public class ImageMessage extends TypeableMessage {

    private FileThumbnail thumbnail;

    /**
     * 构造函数。
     *
     * @param file 指定文件。
     * @param useThumbAsSource 使用缩略图作为源文件。
     */
    public ImageMessage(File file, boolean useThumbAsSource) {
        super(MessageType.Image);

        try {
            this.payload.put("type", MessageTypeName.Image);
        } catch (JSONException e) {
            // Nothing
        }

        this.summary = "[图片]";

        // 创建消息附件
        FileAttachment attachment = new FileAttachment(file);
        attachment.setCompressed(useThumbAsSource);
        this.setAttachment(attachment);
    }

    public ImageMessage(Message message) {
        super(message, MessageType.Image);

        try {
            if (!this.payload.has("type")) {
                this.payload.put("type", MessageTypeName.Image);
            }
        } catch (JSONException e) {
            // Nothing
        }

        this.summary = "[图片]";
    }

    /**
     * 获取文件名。
     *
     * @return 返回文件名。
     */
    public String getFileName() {
        return this.getAttachment().getPrefFileName();
    }

    /**
     * 获取文件大小，单位：字节。
     *
     * @return 返回文件大小。
     */
    public long getFileSize() {
        return this.getAttachment().getPrefFileSize();
    }

    /**
     * 获取文件类型。
     *
     * @return 返回文件类型。
     */
    public String getFileType() {
        return this.getAttachment().getPrefFileType();
    }

    /**
     * 获取文件最近一次修改时间戳。
     *
     * @return 返回文件最近一次修改时间戳。
     */
    public long getFileLastModified() {
        return this.getAttachment().getPrefFileLastModified();
    }

    /**
     * 获取当前文件本地存储路径。
     *
     * @return 返回当前文件本地存储路径。如果文件不在本地返回 {@code null} 值。
     */
    public String getFilePath() {
        return (null != this.getAttachment().getPrefFile()) ?
                this.getAttachment().getPrefFile().getPath() : null;
    }

    /**
     * 判断文件是否在本地存在。
     *
     * @return 如果文件在本地存在返回 {@code true} 。
     */
    public boolean existsLocal() {
        return this.getAttachment().existsPrefLocal();
    }

    /**
     * 获取文件的访问 URL 。
     *
     * @return 返回文件的访问 URL 。
     */
    public String getFileURL() {
        return this.getAttachment().getPrefFileURL();
    }

    /**
     * 获取文件已经被处理的数据大小。
     * 该方法仅在文件消息被处理时有效。
     *
     * @return 返回文件已被处理的数据大小。
     */
    public long getProcessedSize() {
        return this.getAttachment().getPrefProcessedSize();
    }

    /**
     * 获取文件处理进度百分比。
     *
     * @return 返回文件处理进度百分比。
     */
    public int getProgressPercent() {
        return this.getAttachment().getPrefProgressPercent();
    }

    /**
     * 是否有缩略图。
     *
     * @return
     */
    public boolean hasThumbnail() {
        if (null != this.thumbnail) {
            return true;
        }

        FileThumbnail thumb = getThumbnail();
        return (null != thumb);
    }

    /**
     * 获取缩略图。
     *
     * @return
     */
    public FileThumbnail getThumbnail() {
        if (null != this.thumbnail) {
            return this.thumbnail;
        }

        FileLabel label = getAttachment().getPrefFileLabel();
        if (null == label) {
            return null;
        }

        JSONObject thumbJson = label.getContext();
        if (null != thumbJson) {
            try {
                this.thumbnail = new FileThumbnail(thumbJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return this.thumbnail;
    }
}
