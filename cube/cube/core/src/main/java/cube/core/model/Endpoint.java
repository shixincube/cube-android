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

package cube.core.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 接入节点描述。
 */
public class Endpoint extends Entity {

    public final String host;

    public final int port;

    public Endpoint(String host, int port) {
        super();
        this.host = host;
        this.port = port;
    }

    public Endpoint(JSONObject json) throws JSONException {
        super(json);
        this.host = json.getString("host");
        this.port = json.getInt("port");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("host", this.host);
            json.put("port", this.port);
        } catch (JSONException e) {
            // Nothing
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        try {
            json.put("host", this.host);
            json.put("port", this.port);
        } catch (JSONException e) {
            // Nothing
        }
        return json;
    }

    @Override
    public String toString() {
        return this.host + ":" + this.port;
    }
}
