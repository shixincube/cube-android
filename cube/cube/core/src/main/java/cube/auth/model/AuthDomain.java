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

package cube.auth.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cube.core.model.Endpoint;
import cube.core.model.Entity;

/**
 * 授权域。
 */
public class AuthDomain extends Entity {

    public final String domainName;

    public final String appKey;

    public final String appId;

    public final Endpoint mainEndpoint;

    public final Endpoint httpEndpoint;

    public final Endpoint httpsEndpoint;

    public final JSONArray iceServers;

    public AuthDomain(JSONObject json) throws JSONException {
        super(json);
        this.domainName = json.getString("domainName");
        this.appKey = json.getString("appKey");
        this.appId = json.getString("appId");
        this.mainEndpoint = new Endpoint(json.getJSONObject("mainEndpoint"));
        this.httpEndpoint = new Endpoint(json.getJSONObject("httpEndpoint"));
        this.httpsEndpoint = new Endpoint(json.getJSONObject("httpsEndpoint"));
        this.iceServers = json.getJSONArray("iceServers");
    }

    @Override
    public int getMemorySize() {
        int size = super.getMemorySize();
        size += 8 * 7;
        return size;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("domainName", this.domainName);
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
