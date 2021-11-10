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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cube.filestorage.model.FileAnchor;
import cube.filestorage.model.FileLabel;
import cube.util.JSONable;

/**
 * 文件附件。
 */
public class FileAttachment implements JSONable {

    /**
     * 文件句柄。
     */
    private File file;

    /**
     * 文件处理时的记录信息。
     */
    private FileAnchor anchor;

    /**
     * 文件标签。
     */
    private FileLabel label;

    /**
     * 缩略图清单。
     */
    private List<FileThumbnail> thumbs;

    /**
     * 构造函数。
     *
     * @param file
     */
    public FileAttachment(File file) {
        this.file = file;
    }

    /**
     * 构造函数。
     *
     * @param json
     * @throws JSONException
     */
    public FileAttachment(JSONObject json) throws JSONException {
        if (json.has("anchor")) {
            this.anchor = new FileAnchor(json.getJSONObject("anchor"));
        }

        if (json.has("label")) {
            this.label = new FileLabel(json.getJSONObject("label"));
        }

        if (json.has("thumbs")) {
            this.thumbs = new ArrayList<>();
            JSONArray array = json.getJSONArray("thumbs");
            for (int i = 0; i < array.length(); ++i) {
                FileThumbnail thumb = new FileThumbnail(array.getJSONObject(i));
                this.thumbs.add(thumb);
            }
        }
    }

    /**
     * 获取文件实例。
     *
     * @return
     */
    public File getFile() {
        if (null != this.file) {
            return this.file;
        }

        if (null != this.label) {
            String path = this.label.getFilePath();
            if (null != path) {
                this.file = new File(path);
            }
        }
        else if (null != this.anchor) {
            if (null != this.anchor.filePath) {
                this.file = new File(this.anchor.filePath);
            }
        }

        return this.file;
    }

    /**
     * 文件是否在本地存在。
     *
     * @return
     */
    public boolean existsLocal() {
        File file = this.getFile();
        return (null != file && file.exists());
    }

    /**
     * 返回文件码。
     *
     * @return 返回文件码。
     */
    public String getFileCode() {
        if (null != this.label) {
            return this.label.getFileCode();
        }
        else if (null != this.anchor) {
            return this.anchor.getFileCode();
        }
        else {
            return null;
        }
    }

    /**
     * 返回文件名。
     *
     * @return 返回文件名。
     */
    public String getFileName() {
        if (null != this.file) {
            return this.file.getName();
        }
        else if (null != this.label) {
            return this.label.getFileName();
        }
        else if (null != this.anchor) {
            return this.anchor.fileName;
        }

        return null;
    }

    public String getFileType() {
        if (null != this.label) {
            return this.label.getFileType();
        }
        else if (null != this.anchor) {
            return this.anchor.getExtension().toLowerCase(Locale.ROOT);
        }
        else {
            return "unknown";
        }
    }

    public long getFileSize() {
        if (null != this.label) {
            return this.label.getFileSize();
        }
        else if (null != this.anchor) {
            return this.anchor.getFileSize();
        }
        else if (null != this.file) {
            return this.file.length();
        }
        else {
            return 0;
        }
    }

    public long getFileLastModified() {
        if (null != this.file) {
            return this.file.lastModified();
        }
        else if (null != this.label) {
            return this.label.getLastModified();
        }
        else if (null != this.anchor) {
            return this.anchor.getLastModified();
        }
        else {
            return 0;
        }
    }

    /**
     * 文件是否是图像类型文件。
     *
     * @return
     */
    public boolean isImageType() {
        String type = this.getFileType();
        if (type.equalsIgnoreCase("png")
                || type.equalsIgnoreCase("jpg")
                || type.equalsIgnoreCase("gif")
                || type.equalsIgnoreCase("jpeg")
                || type.equalsIgnoreCase("bmp")) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * 在处理状态下获取已处理的文件大小。
     *
     * @return 返回 {@code -1} 表示未被处理。
     */
    public long getProcessedSize() {
        if (null != this.anchor) {
            return this.anchor.position;
        }

        return -1;
    }

    public void setAnchor(FileAnchor anchor) {
        this.anchor = anchor;
    }

    public void setLabel(FileLabel label) {
        this.label = label;
    }

    public FileLabel getLabel() {
        return this.label;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        try {
            if (null != this.label) {
                json.put("label", this.label.toJSON());
            }

            if (null != this.anchor) {
                json.put("anchor", this.anchor.toJSON());
            }

            if (null != this.thumbs) {
                JSONArray array = new JSONArray();
                for (FileThumbnail thumbnail : this.thumbs) {
                    array.put(thumbnail.toJSON());
                }
                json.put("thumbs", array);
            }
        } catch (JSONException e) {
            e.printStackTrace();;
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }


    /**
     * 在服务器端进行缩略图操作的配置信息。
     */
    public class ThumbConfig implements JSONable {

        private float quality = 1.0f;

        public ThumbConfig() {
        }

        @Override
        public JSONObject toJSON() {
            return null;
        }

        @Override
        public JSONObject toCompactJSON() {
            return null;
        }
    }
}
