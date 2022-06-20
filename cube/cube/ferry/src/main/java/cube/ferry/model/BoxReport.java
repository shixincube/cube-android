/*
 * This source file is part of Cube.
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

package cube.ferry.model;

import org.json.JSONException;
import org.json.JSONObject;

import cube.core.model.Entity;

/**
 * 报告。
 */
public class BoxReport extends Entity {

    /**
     * 数据空间大小。
     */
    private long dataSpaceSize;

    private long imageFilesUsedSize;

    private long docFilesUsedSize;

    private long videoFilesUsedSize;

    private long audioFilesUsedSize;

    private long packageFilesUsedSize;

    private long otherFilesUsedSize;

    /**
     * 空闲磁盘空间。
     */
    private long freeDiskSize;

    public BoxReport(String domainName) {
        super();
    }

    public BoxReport(JSONObject json) throws JSONException {
        super(json);
        this.dataSpaceSize = json.getLong("dataSpaceSize");
        this.freeDiskSize = json.getLong("freeDiskSize");
        this.imageFilesUsedSize = json.getLong("imageFilesUsedSize");
        this.docFilesUsedSize = json.getLong("docFilesUsedSize");
        this.videoFilesUsedSize = json.getLong("videoFilesUsedSize");
        this.audioFilesUsedSize = json.getLong("audioFilesUsedSize");
        this.packageFilesUsedSize = json.getLong("packageFilesUsedSize");
        this.otherFilesUsedSize = json.getLong("otherFilesUsedSize");
    }

    public void setDataSpaceSize(long size) {
        this.dataSpaceSize = size;
    }

    public long getDataSpaceSize() {
        return this.dataSpaceSize;
    }

    public void setFreeDiskSize(long size) {
        this.freeDiskSize = size;
    }

    public long getFreeDiskSize() {
        return this.freeDiskSize;
    }

    public void setImageFilesUsedSize(long size) {
        this.imageFilesUsedSize = size;
    }

    public long getImageFilesUsedSize() {
        return this.imageFilesUsedSize;
    }

    public void setDocFilesUsedSize(long size) {
        this.docFilesUsedSize = size;
    }

    public long getDocFilesUsedSize() {
        return this.docFilesUsedSize;
    }

    public void setVideoFilesUsedSize(long size) {
        this.videoFilesUsedSize = size;
    }

    public long getVideoFilesUsedSize() {
        return this.videoFilesUsedSize;
    }

    public void setAudioFilesUsedSize(long size) {
        this.audioFilesUsedSize = size;
    }

    public long getAudioFilesUsedSize() {
        return this.audioFilesUsedSize;
    }

    public void setPackageFilesUsedSize(long size) {
        this.packageFilesUsedSize = size;
    }

    public long getPackageFilesUsedSize() {
        return this.packageFilesUsedSize;
    }

    public void setOtherFilesUsedSize(long size) {
        this.otherFilesUsedSize = size;
    }

    public long getOtherFilesUsedSize() {
        return this.otherFilesUsedSize;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("dataSpaceSize", this.dataSpaceSize);
            json.put("freeDiskSize", this.freeDiskSize);
            json.put("imageFilesUsedSize", this.imageFilesUsedSize);
            json.put("docFilesUsedSize", this.docFilesUsedSize);
            json.put("videoFilesUsedSize", this.videoFilesUsedSize);
            json.put("audioFilesUsedSize", this.audioFilesUsedSize);
            json.put("packageFilesUsedSize", this.packageFilesUsedSize);
            json.put("otherFilesUsedSize", this.otherFilesUsedSize);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}