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

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cube.filestorage.model.FileAnchor;
import cube.filestorage.model.FileLabel;
import cube.util.JSONable;

/**
 * 文件附件。
 */
public class FileAttachment implements JSONable {

    /**
     * 文件处理时的记录信息。
     */
    private List<FileAnchor> anchorList;

    /**
     * 文件标签。
     */
    private List<FileLabel> labelList;

    /**
     * 是否压缩了原文件。
     */
    private boolean compressed = false;

    /**
     * 构造函数。
     *
     * @param file
     */
    public FileAttachment(File file) {
        this.reset(file);
    }

    /**
     * 构造函数。
     *
     * @param json
     * @throws JSONException
     */
    public FileAttachment(JSONObject json) throws JSONException {
        this.anchorList = new ArrayList<>();
        this.labelList = new ArrayList<>();

        if (json.has("labels")) {
            JSONArray array = json.getJSONArray("labels");
            for (int i = 0; i < array.length(); ++i) {
                FileLabel fileLabel = new FileLabel(array.getJSONObject(i));
                this.labelList.add(fileLabel);
            }
        }

        if (json.has("anchors")) {
            JSONArray array = json.getJSONArray("anchors");
            for (int i = 0; i < array.length(); ++i) {
                FileAnchor fileAnchor = new FileAnchor(array.getJSONObject(i));
                this.anchorList.add(fileAnchor);
            }
        }

        if (json.has("compressed")) {
            this.compressed = json.getBoolean("compressed");
        }
    }

    public File getPrefFile() {
        return this.getFile(0);
    }

    /**
     * 获取文件实例。
     *
     * @return
     */
    public File getFile(int index) {
        File file = null;
        if (index < this.anchorList.size()) {
            file = this.anchorList.get(index).getFile();
        }

        if (null == file || !file.exists()) {
            if (index < this.labelList.size()) {
                String path = this.labelList.get(index).getFilePath();
                if (null != path) {
                    file = new File(path);
                }
            }
        }

        return file;
    }

    /**
     * 文件是否在本地存在。
     *
     * @return
     */
    public boolean existsPrefLocal() {
        return this.existsLocal(0);
    }

    public boolean existsLocal(int index) {
        File file = this.getFile(index);
        return (null != file && file.exists());
    }

    /**
     * 返回文件码。
     *
     * @return 返回文件码。
     */
    public String getPrefFileCode() {
        return this.getFileCode(0);
    }

    public String getFileCode(int index) {
        if (index < this.labelList.size()) {
            return this.labelList.get(index).getFileCode();
        }

        if (index < this.anchorList.size()) {
            return this.anchorList.get(index).getFileCode();
        }

        return null;
    }

    /**
     * 返回文件名。
     *
     * @return 返回文件名。
     */
    public String getPrefFileName() {
        return this.getFileName(0);
    }

    public String getFileName(int index) {
        if (index < this.labelList.size()) {
            return this.labelList.get(index).getFileName();
        }

        if (index < this.anchorList.size()) {
            return this.anchorList.get(index).getFileName();
        }

        return null;
    }

    public String getPrefFileType() {
        return this.getFileType(0);
    }

    /**
     * 获取文件类型。
     *
     * @return
     */
    public String getFileType(int index) {
        if (index < this.anchorList.size()) {
            return this.anchorList.get(index).getExtension();
        }

        if (index < this.labelList.size()) {
            return this.labelList.get(index).getFileType();
        }

        return "unknown";
    }

    public long getPrefFileSize() {
        return this.getFileSize(0);
    }

    /**
     * 获取文件大小。
     *
     * @return
     */
    public long getFileSize(int index) {
        if (index < this.labelList.size()) {
            return this.labelList.get(index).getFileSize();
        }

        if (index < this.anchorList.size()) {
            return this.anchorList.get(index).getFileSize();
        }

        return 0;
    }

    public long getPrefFileLastModified() {
        return this.getFileLastModified(0);
    }

    /**
     * 获取文件最后一次修改时间戳。
     *
     * @return
     */
    public long getFileLastModified(int index) {
        if (index < this.labelList.size()) {
            return this.labelList.get(index).getLastModified();
        }

        if (index < this.anchorList.size()) {
            return this.anchorList.get(index).getLastModified();
        }

        return 0;
    }

    /**
     * 获取文件的 URL 。
     *
     * @return
     */
    public String getPrefFileURL() {
        return this.getFileURL(0);
    }

    public String getFileURL(int index) {
        return this.existsLocal(index) ? Uri.fromFile(getFile(index)).toString() :
                this.getFileLabel(index).getURL();
    }

    public boolean isPrefImageType() {
        return this.isImageType(0);
    }

    /**
     * 文件是否是图像类型文件。
     *
     * @return
     */
    public boolean isImageType(int index) {
        String type = this.getFileType(index);
        if (type.equalsIgnoreCase("png")
                || type.equalsIgnoreCase("jpg")
                || type.equalsIgnoreCase("gif")
                || type.equalsIgnoreCase("jpeg")
                || type.equalsIgnoreCase("webp")
                || type.equalsIgnoreCase("bmp")) {
            return true;
        }
        else {
            return false;
        }
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    public boolean isCompressed() {
        return this.compressed;
    }

    public FileLabel getPrefFileLabel() {
        return this.getFileLabel(0);
    }

    public FileLabel getFileLabel(int index) {
        if (index < this.labelList.size()) {
            return this.labelList.get(index);
        }

        return null;
    }

    public FileAnchor getPrefFileAnchor() {
        return this.getFileAnchor(0);
    }

    public FileAnchor getFileAnchor(int index) {
        if (index < this.anchorList.size()) {
            return this.anchorList.get(index);
        }

        return null;
    }

    public long getPrefProcessedSize() {
        return this.getProcessedSize(0);
    }

    /**
     * 在处理状态下获取已处理的文件大小。
     *
     * @return 返回 {@code -1} 表示未被处理。
     */
    public long getProcessedSize(int index) {
        if (null != this.getFileAnchor(index)) {
            return this.getFileAnchor(index).position;
        }

        return -1;
    }

    public int getPrefProgressPercent() {
        return this.getProgressPercent(0);
    }

    /**
     * 获取文件的处理进度百分比。
     *
     * @return 返回文件的处理进度百分比。
     */
    public int getProgressPercent(int index) {
        FileAnchor anchor = this.getFileAnchor(index);
        if (null != anchor) {
            if (anchor.position == anchor.getFileSize()) {
                return 100;
            }

            return (int) Math.floor((double) anchor.position / (double) anchor.getFileSize() * 100.0f);
        }
        else {
            return 100;
        }
    }

    public int numAnchors() {
        return this.anchorList.size();
    }

    /**
     * 重置当前附录。不重置压缩标识。
     *
     * @param file
     */
    public void reset(File file) {
        this.anchorList = new ArrayList<>();
        this.labelList = new ArrayList<>();

        FileAnchor anchor = new FileAnchor(file);
        this.anchorList.add(anchor);
        FileLabel label = new FileLabel(anchor);
        this.labelList.add(label);
    }

    public void matchFileLabel(FileAnchor anchor, FileLabel label) {
        int index = this.anchorList.indexOf(anchor);
        if (index >= 0) {
            this.labelList.set(index, label);
        }
    }

    public void update(FileAttachment attachment) {
        for (int i = 0; i < attachment.labelList.size(); ++i) {
            FileLabel newLabel = attachment.labelList.get(i);
            FileLabel current = this.getFileLabel(i);
            if (null == current) {
                newLabel.setFilePath(this.anchorList.get(i).getFilePath());
                this.labelList.add(newLabel);
            }
            else {
                // 设置新标签的文件路径
                String path = current.getFilePath();
                if (null == path) {
                    path = this.anchorList.get(i).getFilePath();
                }
                newLabel.setFilePath(path);
                this.labelList.set(i, newLabel);
            }
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        try {
            JSONArray array = new JSONArray();
            for (FileAnchor anchor : this.anchorList) {
                array.put(anchor.toJSON());
            }
            json.put("anchors", array);

            array = new JSONArray();
            for (FileLabel label : this.labelList) {
                array.put(label.toJSON());
            }
            json.put("labels", array);

            json.put("compressed", this.compressed);
        } catch (JSONException e) {
            e.printStackTrace();;
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
