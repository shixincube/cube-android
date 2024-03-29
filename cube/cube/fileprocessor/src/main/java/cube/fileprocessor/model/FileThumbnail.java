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

package cube.fileprocessor.model;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

import cube.auth.AuthService;
import cube.core.model.Entity;
import cube.filestorage.model.FileLabel;
import cube.util.JSONable;

/**
 * 文件缩略图。
 */
public class FileThumbnail extends Entity {

    /**
     * 本地文件。
     */
    private File file;

    /**
     * 本地文件路径。
     */
    private String filePath;

    /**
     * 缩略图文件标签。
     */
    private FileLabel fileLabel;

    /**
     * 缩略图宽度。
     */
    private int width;

    /**
     * 缩略图高度。
     */
    private int height;

    /**
     * 缩略图使用的图像质量参数。
     */
    private int quality;

    /**
     * 原文件文件码。
     */
    private String sourceFileCode;

    /**
     * 原文件宽度。
     */
    private int sourceWidth;

    /**
     * 原文件高度。
     */
    private int sourceHeight;

    /**
     * 构造函数。
     *
     * @param file
     */
    public FileThumbnail(File file, int width, int height, int quality, int sourceWidth, int sourceHeight) {
        this.file = file;
        this.filePath = file.getAbsolutePath();
        this.width = width;
        this.height = height;
        this.quality = quality;
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
    }

    /**
     * 构造函数。
     *
     * @param json
     * @throws JSONException
     */
    public FileThumbnail(JSONObject json) throws JSONException {
        super(json);

        if (json.has("fileLabel")) {
            this.fileLabel = new FileLabel(json.getJSONObject("fileLabel"));
        }

        this.width = json.getInt("width");
        this.height = json.getInt("height");
        this.sourceFileCode = json.getString("sourceFileCode");
        this.sourceWidth = json.getInt("sourceWidth");
        this.sourceHeight = json.getInt("sourceHeight");
        this.quality = json.getInt("quality");

        if (json.has("filePath")) {
            this.filePath = json.getString("filePath");
            this.file = new File(this.filePath);
            if (!this.file.exists() || this.file.length() == 0) {
                this.file = null;
            }
        }

        if (null == this.file && null != this.fileLabel) {
            String path = this.fileLabel.getFilePath();
            if (null != path) {
                this.file = new File(path);
                this.filePath = this.file.getAbsolutePath();
            }
        }
    }

    /**
     * 获取本地文件。
     *
     * @return
     */
    public File getFile() {
        return this.file;
    }

    /**
     * 获取文件标签。
     *
     * @return
     */
    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    /**
     * 文件是否在本地存在。
     *
     * @return
     */
    public boolean existsLocal() {
        return (null != this.file && this.file.exists() && this.file.length() > 0);
    }

    /**
     * 获取访问文件的 URL 字符串。
     *
     * @return
     */
    public String getFileURL() {
        if (null != this.file && this.file.exists() && this.file.length() > 0) {
            return Uri.fromFile(this.file).toString();
        }
        else if (null != this.fileLabel) {
            return this.fileLabel.getURL();
        }
        else {
            return null;
        }
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getQuality() {
        return this.quality;
    }

    public void resetFile(File file) {
        this.file = file;
        this.filePath = file.getAbsolutePath();
        if (null != this.fileLabel) {
            this.fileLabel.setFilePath(this.filePath);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (null != object && object instanceof FileThumbnail) {
            FileThumbnail other = (FileThumbnail) object;
            if (null != other.fileLabel && null != this.fileLabel) {
                return this.fileLabel.getFileCode().equals(other.fileLabel.getFileCode());
            }
            else if (null != other.filePath && null != this.filePath) {
                return this.filePath.equals(other.filePath);
            }
        }

        return false;
    }

    public String print() {
        StringBuilder buf = new StringBuilder();
        if (null != this.filePath) {
            buf.append("\"").append(this.filePath).append("\" - ");
        }
        buf.append("(").append(this.sourceWidth).append("x").append(this.sourceHeight).append(")");
        buf.append(" -> ");
        buf.append("(").append(this.width).append("x").append(this.height).append(")");
        buf.append(" # ").append(String.format(Locale.ROOT, "%d", this.quality));
        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toCompactJSON();
        try {
            json.put("domain", AuthService.getDomain());

            if (null != this.fileLabel) {
                json.put("fileLabel", this.fileLabel.toJSON());
            }

            json.put("width", this.width);
            json.put("height", this.height);
            json.put("sourceFileCode", (null != this.sourceFileCode) ? this.sourceFileCode : "");
            json.put("sourceWidth", this.sourceWidth);
            json.put("sourceHeight", this.sourceHeight);
            json.put("quality", this.quality);

            if (null != this.filePath) {
                json.put("filePath", this.filePath);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    /**
     * 缩略图操作的配置信息。
     */
    public class ThumbConfig implements JSONable {

        public int quality = 60;

        public ThumbConfig() {
        }

        public ThumbConfig(int quality) {
            this.quality = quality;
        }

        public ThumbConfig(JSONObject json) throws JSONException {
            this.quality = json.getInt("quality");
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            try {
                json.put("quality", this.quality);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
        }
    }
}
