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

import cube.contact.model.Contact;
import cube.contact.model.Device;
import cube.core.model.Entity;

/**
 * 文件分享标签。
 */
public class SharingTag extends Entity {

    private String code;

    private String httpURL;

    private String httpsURL;

    private Contact sharer;

    private Device device;

    private FileLabel fileLabel;

    private long duration;

    private long expiryDate;

    private String password;

    public SharingTag(JSONObject json) throws JSONException {
        super(json);
        this.code = json.getString("code");
        this.expiryDate = json.getLong("expiryDate");

        this.httpURL = json.has("httpURL") ? json.getString("httpURL") : null;
        this.httpsURL = json.has("httpsURL") ? json.getString("httpsURL") : null;

        JSONObject config = json.getJSONObject("config");
        if (config.has("contact")) {
            this.sharer = new Contact(config.getJSONObject("contact"));
        }
        this.device = new Device(config.getJSONObject("device"));
        this.fileLabel = new FileLabel(config.getJSONObject("fileLabel"));
        this.duration = config.getLong("duration");

        if (config.has("password")) {
            this.password = config.getString("password");
        }
    }

    public String getCode() {
        return this.code;
    }

    public String getHttpURL() {
        return this.httpURL;
    }

    public String getHttpsURL() {
        return this.httpsURL;
    }

    public Contact getSharer() {
        return this.sharer;
    }

    public Device getDevice() {
        return this.device;
    }

    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    public long getExpiryDate() {
        return this.expiryDate;
    }

    public long getDuration() {
        return this.duration;
    }

    public String getPassword() {
        return this.password;
    }
}
