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

package com.shixincube.app.model;

import org.json.JSONException;
import org.json.JSONObject;

import cube.util.JSONable;

/**
 * 应用程序版本信息。
 */
public class AppVersion implements JSONable {

    private String device;

    private int major;

    private int minor;

    private int revision;

    private String build;

    private boolean important;

    private String download;

    public AppVersion(String device, int major, int minor, int revision, boolean important) {
        this.device = device;
        this.major = major;
        this.minor = minor;
        this.revision = revision;
        this.important = important;
    }

    public AppVersion(JSONObject json) {
        try {
            this.device = json.getString("device");
            this.major = json.getInt("major");
            this.minor = json.getInt("minor");
            this.revision = json.getInt("revision");
            this.important = json.getBoolean("important");

            if (json.has("build")) {
                this.build = json.getString("build");
            }

            if (json.has("download")) {
                this.download = json.getString("download");
            }
        } catch (JSONException e) {
            // Nothing
        }
    }

    public String getDevice() {
        return this.device;
    }

    public int getMajor() {
        return this.major;
    }

    public int getMinor() {
        return this.minor;
    }

    public int getRevision() {
        return this.revision;
    }

    public String getBuild() {
        return this.build;
    }

    public void setBuild(String value) {
        this.build = value;
    }

    public boolean isImportant() {
        return this.important;
    }

    public String getDownload() {
        return this.download;
    }

    public void setDownload(String value) {
        this.download = value;
    }

    public String getDescription() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.major);
        buf.append(".");
        buf.append(this.minor);
        buf.append(".");
        buf.append(this.revision);
        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("device", this.device);
            json.put("major", this.major);
            json.put("minor", this.minor);
            json.put("revision", this.revision);
            json.put("important", this.important);

            if (null != this.build) {
                json.put("build", this.build);
            }

            if (null != this.download) {
                json.put("download", this.download);
            }
        } catch (JSONException e) {
            // Nothing
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
