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

package cube.filestorage.model;

import org.json.JSONException;
import org.json.JSONObject;

import cube.auth.AuthService;
import cube.core.model.Entity;

/**
 * 文件标签。用于标记文件的基本信息。
 */
public class FileLabel extends Entity {

    public final String domain;

    /**
     * 文件所有人 ID 。
     */
    private Long ownerId;

    /**
     * 文件名。
     */
    private String fileName;

    /**
     * 文件大小。单位：字节。
     */
    private long fileSize;

    /**
     * 文件最后一次修改时间。
     */
    private long lastModified;

    /**
     * 文件完成处理的时间。
     */
    private long completedTime;

    /**
     * 文件到期时间。
     */
    private long expiryTime;

    /**
     * 文件码。
     */
    private String fileCode;

    /**
     * 文件类型。
     */
    private String fileType = "unknown";

    /**
     * 文件 MD5 码。
     */
    private String md5Code;

    /**
     * 文件 SHA1 码。
     */
    private String sha1Code;

    /**
     * 文件的访问 URL 。
     */
    private String fileURL;

    /**
     * 文件的安全访问 URL 。
     */
    private String fileSecureURL;

    public FileLabel() {
        this.domain = AuthService.getDomain();
    }

    public FileLabel(JSONObject json) throws JSONException {
        super(json);
        this.domain = json.getString("domain");
        this.fileCode = json.getString("fileCode");
        this.ownerId = json.getLong("ownerId");
        this.fileName = json.getString("fileName");
        this.fileSize = json.getLong("fileSize");
        this.lastModified = json.getLong("lastModified");
        this.completedTime = json.getLong("completedTime");
        this.expiryTime = json.getLong("expiryTime");
        this.fileType = json.getString("fileType");
        this.md5Code = json.has("md5") ? json.getString("md5") : null;
        this.sha1Code = json.has("sha1") ? json.getString("sha1") : null;
        this.fileURL = json.has("fileURL") ? json.getString("fileURL") : null;
        this.fileSecureURL = json.has("fileSecureURL") ? json.getString("fileSecureURL") : null;

        if (this.fileType.equalsIgnoreCase("unknown")) {
            int index = this.fileName.lastIndexOf(".");
            if (index > 0) {
                String extension = this.fileName.substring(index + 1);
                this.fileType = extension;
            }
        }
    }

    public String getFileCode() {
        return this.fileCode;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getFileType() {
        return this.fileType;
    }
}
