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

import java.io.IOException;
import java.io.InputStream;

import cube.core.model.Entity;
import cube.filestorage.handler.UploadFileHandler;

/**
 * 文件锚点。
 */
public class FileAnchor extends Entity {

    /**
     * 文件名。
     */
    public final String filename;

    /**
     * 文件大小，单位：字节。
     */
    public final long fileSize;

    /**
     * 文件最近一次修改时间。
     */
    public final long lastModified;

    /**
     * 当前锚点处理的游标位置，即已处理的数据大小。
     */
    public long position;

    /**
     * 剩余大小。
     */
    private long remaining;

    /**
     * 文件码。
     */
    private String fileCode;

    /**
     * 上传文件句柄。
     */
    private UploadFileHandler handler;

    public InputStream inputStream;

    public FileAnchor(String filename, long fileSize, long lastModified) {
        super();
        this.filename = filename;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
        this.position = 0;
        this.remaining = 0;
    }

    public void setFileCode(String fileCode) {
        this.fileCode = fileCode;
    }

    public String getFileCode() {
        return this.fileCode;
    }

    public void setUploadFileHandler(UploadFileHandler handler) {
        this.handler = handler;
    }

    public UploadFileHandler getUploadFileHandler() {
        return this.handler;
    }

    public void bindInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void updatePosition(int size) {
        this.position += size;
        this.remaining = this.fileSize - this.position;
    }

    public long getRemaining() {
        return this.remaining;
    }

    public boolean isFinish() {
        return this.position == this.fileSize;
    }

    public void close() {
        if (null != this.inputStream) {
            try {
                this.inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
