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

import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import cube.auth.AuthService;
import cube.core.Kernel;
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
     * 文件的本地路径。
     */
    private String filePath;

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

    /**
     * 构造函数。
     */
    public FileLabel(FileAnchor anchor) {
        super();
        this.domain = AuthService.getDomain();
        this.filePath = anchor.getFilePath();
        this.fileCode = anchor.getFileCode();
        this.fileName = anchor.getFileName();
        this.fileSize = anchor.getFileSize();
        this.lastModified = anchor.getLastModified();
    }

    /**
     * 构造函数。
     *
     * @param id
     * @param timestamp
     * @param ownerId
     * @param fileCode
     * @param filePath
     * @param fileName
     * @param fileSize
     * @param lastModified
     * @param completedTime
     * @param expiryTime
     * @param fileType
     * @param md5Code
     * @param sha1Code
     * @param url
     * @param secureURL
     */
    public FileLabel(Long id, long timestamp, Long ownerId, String fileCode, String filePath,
                     String fileName, long fileSize, long lastModified, long completedTime, long expiryTime,
                     String fileType, String md5Code, String sha1Code, String url, String secureURL) {
        super(id, timestamp);
        this.domain = AuthService.getDomain();
        this.ownerId = ownerId;
        this.fileCode = fileCode;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
        this.completedTime = completedTime;
        this.expiryTime = expiryTime;
        this.fileType = fileType;
        this.md5Code = md5Code;
        this.sha1Code = sha1Code;
        this.fileURL = url;
        this.fileSecureURL = secureURL;
    }

    public FileLabel(JSONObject json) throws JSONException {
        super(json);
        this.domain = json.getString("domain");
        this.ownerId = json.getLong("ownerId");
        this.fileCode = json.getString("fileCode");
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

        if (json.has("filePath")) {
            this.filePath = json.getString("filePath");
        }
    }

    /**
     * 获取文件所有人 ID 。
     *
     * @return 返回文件所有人 ID 。
     */
    public Long getOwnerId() {
        return this.ownerId;
    }

    /**
     * 获取文件的文件码。
     *
     * @return 返回文件的文件码。
     */
    public String getFileCode() {
        return this.fileCode;
    }

    /**
     * 获取文件在本地的存储路径。
     *
     * @return 返回文件的本地存放路径。
     */
    public String getFilePath() {
        return this.filePath;
    }

    /**
     * 获取文件的原文件名。
     *
     * @return 返回文件的原文件名。
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * 获取文件的类型描述。
     *
     * @return 返回文件的类型描述。
     */
    public String getFileType() {
        return this.fileType;
    }

    /**
     * 获取文件大小。
     *
     * @return 返回文件大小。
     */
    public long getFileSize() {
        return this.fileSize;
    }

    /**
     * 获取文件最后一次修改时间。
     *
     * @return 返回文件最后一次修改时间。
     */
    public long getLastModified() {
        return this.lastModified;
    }

    /**
     * 获取文件在服务器处理完成时间戳。
     *
     * @return 返回文件在服务器处理完成时间戳。
     */
    public long getCompletedTime() {
        return this.completedTime;
    }

    /**
     * 获取文件的超期时间戳。
     *
     * @return 返回文件的超期时间戳。
     */
    public long getExpiryTime() {
        return this.expiryTime;
    }

    /**
     * 获取文件的 MD5 散列码。
     *
     * @return 返回文件的 MD5 散列码。
     */
    public String getMd5Code() {
        return this.md5Code;
    }

    /**
     * 获取文件的 SHA1 散列码。
     *
     * @return 返回文件的 SHA1 散列码。
     */
    public String getSha1Code() {
        return this.sha1Code;
    }

    /**
     * 获取文件的 HTTP 访问 URL 串。
     *
     * @return 返回文件的 HTTP 访问 URL 串。
     */
    public String getURL() {
        String url = this.fileURL;
        if (Build.SERIAL.contains("unknown")) {
            // FIXME 以下判断仅用于测试，Release 时务必使用域名
            // 模拟器里将 127.0.0.1 修改为 10.0.2.2
            url = this.fileURL.replace("127.0.0.1", "10.0.2.2");
        }
        return url + "&token=" + Kernel.getDefault().getAuthToken().code + "&type=" + this.fileType;
    }

    /**
     * 获取文件的 HTTPS 访问 URL 串。
     *
     * @return 返回文件的 HTTPS 访问 URL 串。
     */
    public String getSecureURL() {
        String url = this.fileSecureURL;
        if (Build.SERIAL.contains("unknown")) {
            // FIXME 以下判断仅用于测试，Release 时务必使用域名
            // 模拟器里将 127.0.0.1 修改为 10.0.2.2
            url = this.fileSecureURL.replace("127.0.0.1", "10.0.2.2");
        }
        return url + "&token=" + Kernel.getDefault().getAuthToken().code + "&type=" + this.fileType;
    }

    /**
     * <b>Non-public API</b>
     * @param filePath
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public int getMemorySize() {
        int size = super.getMemorySize();

        size += 8 * 14;

        size += this.fileName.getBytes(StandardCharsets.UTF_8).length;
        size += this.fileCode.getBytes(StandardCharsets.UTF_8).length;

        if (null != this.filePath) {
            size += this.filePath.getBytes(StandardCharsets.UTF_8).length;
        }

        size += this.fileType.getBytes(StandardCharsets.UTF_8).length;

        if (null != this.md5Code) {
            size += this.md5Code.getBytes(StandardCharsets.UTF_8).length;
        }
        if (null != this.sha1Code) {
            size += this.sha1Code.getBytes(StandardCharsets.UTF_8).length;
        }

        if (null != this.fileURL) {
            size += this.fileURL.getBytes(StandardCharsets.UTF_8).length;
        }
        if (null != this.fileSecureURL) {
            size += this.fileSecureURL.getBytes(StandardCharsets.UTF_8).length;
        }

        return size;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toCompactJSON();
        try {
            json.put("domain", this.domain);
            json.put("ownerId", this.ownerId);
            json.put("fileCode", this.fileCode);
            json.put("fileName", this.fileName);
            json.put("fileSize", this.fileSize);
            json.put("lastModified", this.lastModified);
            json.put("completedTime", this.completedTime);
            json.put("expiryTime", this.expiryTime);
            json.put("fileType", this.fileType);

            if (null != this.md5Code) {
                json.put("md5", this.md5Code);
            }

            if (null != this.sha1Code) {
                json.put("sha1", this.sha1Code);
            }

            if (null != this.fileURL) {
                json.put("fileURL", this.fileURL);
            }

            if (null != this.fileSecureURL) {
                json.put("fileSecureURL", this.fileSecureURL);
            }

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
}
