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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cube.auth.AuthService;

/**
 * 用户描述自己的账号。
 */
public class Self extends Contact {

    public final Device device;

    public Self(Long id, String name) {
        super(id, name, AuthService.getDomain());
        this.device = new Device();
    }

    public Self(Long id, String name, JSONObject context) {
        super(id, name, AuthService.getDomain(), context);
        this.device = new Device();
    }

    public Self(JSONObject json) throws JSONException {
        super(json);
        this.device = new Device(json.getJSONObject("device"));
    }

    public void update(JSONObject json) throws JSONException {
        this.name = json.getString("name");

        if (json.has("context")) {
            this.context = json.getJSONObject("context");
        }

        if (json.has("devices")) {
            JSONArray array = json.getJSONArray("devices");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject data = array.getJSONObject(i);
                this.addDevice(new Device(data));
            }
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("device", this.device.toJSON());
        } catch (JSONException e) {
            // Nothing
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        try {
            json.put("device", this.device.toJSON());
        } catch (JSONException e) {
            // Nothing
        }
        return json;
    }
}
