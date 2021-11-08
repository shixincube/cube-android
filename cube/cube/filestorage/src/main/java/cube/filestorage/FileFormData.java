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

package cube.filestorage;

import cube.auth.AuthService;

/**
 * 文件的表单数据。
 */
public class FileFormData extends FormData {

    protected final static String FIELD_CID = "cid";
    protected final static String FIELD_DOMAIN = "domain";
    protected final static String FIELD_FILE_SIZE = "fileSize";
    protected final static String FIELD_LAST_MODIFIED = "lastModified";
    protected final static String FIELD_CURSOR = "cursor";
    protected final static String FIELD_SIZE = "size";

    private String filename;

    public FileFormData(Long contactId, String filename, long fileSize, long lastModified,
                        long cursor, int size) {
        super();
        this.filename = filename;
        this.setContentDisposition(FIELD_CID, contactId);
        this.setContentDisposition(FIELD_DOMAIN, AuthService.getDomain());
        this.setContentDisposition(FIELD_FILE_SIZE, fileSize);
        this.setContentDisposition(FIELD_LAST_MODIFIED, lastModified);
        this.setContentDisposition(FIELD_CURSOR, cursor);
        this.setContentDisposition(FIELD_SIZE, size);
    }

    public void setData(byte[] data, int offset, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(data, offset, bytes, 0, length);
        this.setFileData(this.filename, bytes);
    }
}
