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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cell.util.collection.FlexibleByteBuffer;

/**
 * 表单数据。
 */
public class FormData {

    private final static String sLineBreak = "\r\n";
    private final static byte[] sLineBreakBytes = sLineBreak.getBytes(StandardCharsets.UTF_8);
    private final static String sDispositionPrefix = "Content-Disposition: form-data; name=";
    private final static byte[] sDispositionPrefixBytes = sDispositionPrefix.getBytes(StandardCharsets.UTF_8);
    private final static byte sQuotation = '"';

    protected String boundary = "----------------CubeFormBoundary" + cell.util.Utils.randomString(16);

    private Map<String, String> fieldMap;

    private String filename;
    private byte[] binaryData;

    private FormDataInputStream dataStream;

    public FormData() {
        this.fieldMap = new HashMap<>();
    }

    public void setContentDisposition(String name, String value) {
        this.fieldMap.put(name, value);
    }

    public void setContentDisposition(String name, long value) {
        this.fieldMap.put(name, Long.toString(value));
    }

    public void setContentDisposition(String name, int value) {
        this.fieldMap.put(name, Integer.toString(value));
    }

    public void setFileData(String filename, byte[] data) {
        this.filename = filename;
        this.binaryData = data;
    }

    public String getBoundary() {
        return this.boundary;
    }

    public InputStream getInputStream() {
        if (null == this.dataStream) {
            this.dataStream = new FormDataInputStream();
        }
        return this.dataStream;
    }

    public String print() {
        InputStream is = this.getInputStream();
        return new String(this.dataStream.buffer.array(), 0, this.dataStream.buffer.limit(), StandardCharsets.UTF_8);
    }

    protected class FormDataInputStream extends InputStream {

        protected FlexibleByteBuffer buffer;

        public FormDataInputStream() {
            byte[] boundaryBytes = boundary.getBytes(StandardCharsets.UTF_8);
            this.buffer = new FlexibleByteBuffer();

            // 写入字段
            Iterator<Map.Entry<String, String>> iterator = fieldMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                String name = entry.getKey();
                String value = entry.getValue();

                buffer.put(boundaryBytes);
                buffer.put(sLineBreakBytes);
                buffer.put(sDispositionPrefixBytes);
                buffer.put(sQuotation);
                buffer.put(name.getBytes(StandardCharsets.UTF_8));
                buffer.put(sQuotation);
                buffer.put(sLineBreakBytes);
                buffer.put(sLineBreakBytes);

                buffer.put(value.getBytes(StandardCharsets.UTF_8));
                buffer.put(sLineBreakBytes);
            }

            // 写入流
            if (null != binaryData) {
                buffer.put(boundaryBytes);
                buffer.put(sLineBreakBytes);
                buffer.put(sDispositionPrefixBytes);
                buffer.put(sQuotation);
                buffer.put("file".getBytes(StandardCharsets.UTF_8));
                buffer.put(sQuotation);
                buffer.put("; filename=".getBytes(StandardCharsets.UTF_8));
                buffer.put(sQuotation);
                buffer.put(filename.getBytes(StandardCharsets.UTF_8));
                buffer.put(sQuotation);
                buffer.put(sLineBreakBytes);

                buffer.put("Content-Type: application/octet-stream".getBytes(StandardCharsets.UTF_8));
                buffer.put(sLineBreakBytes);
                buffer.put(sLineBreakBytes);

                // 二进制数据
                buffer.put(binaryData);

                buffer.put(sLineBreakBytes);
                buffer.put(boundaryBytes);
                buffer.put("--".getBytes(StandardCharsets.UTF_8));
            }

            buffer.flip();
        }

        @Override
        public int read() throws IOException {
            if (this.buffer.position() >= this.buffer.limit()) {
                return -1;
            }

            return this.buffer.get();
        }
    }
}
