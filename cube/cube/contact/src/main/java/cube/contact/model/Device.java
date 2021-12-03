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

package cube.contact.model;

import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import cube.core.Kernel;
import cube.core.model.Cacheable;
import cube.util.JSONable;

/**
 * 设备实体。
 */
public class Device implements JSONable, Cacheable {

    public final String name;

    public final String platform;

    public Device() {
        this.name = "Android";
        this.platform = Build.MODEL + " " + Build.VERSION.RELEASE
                + "/" + Build.BRAND
                + "/" + Kernel.getDefault().getDeviceSerial();
    }

    public Device(String name, String platform) {
        this.name = name;
        this.platform = platform;
    }

    public Device(JSONObject json) throws JSONException {
        this.name = json.getString("name");
        this.platform = json.getString("platform");
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (null == object || !(object instanceof Device)) {
            return false;
        }

        Device other = (Device) object;
        return (other.name.equals(this.name) && other.platform.equals(this.platform));
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() + this.platform.hashCode();
    }

    @Override
    public int getMemorySize() {
        int size = 8 + 8 + 8;
        size += this.name.getBytes(StandardCharsets.UTF_8).length;
        size += this.platform.getBytes(StandardCharsets.UTF_8).length;
        return size;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("name", this.name);
            json.put("platform", this.platform);
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
