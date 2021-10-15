/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Shixin Cube Team.
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

package cube.auth;

import org.json.JSONException;
import org.json.JSONObject;

import cube.util.JSONable;

/**
 * 主访问信息。
 */
public final class PrimaryDescription implements JSONable {

    public final String address;

    public final int port;

    /**
     * 主要的控制和访问数据内容。
     */
    public final JSONObject primaryContent;

    public PrimaryDescription(JSONObject data) throws JSONException {
        this.address = data.getString("address");
        this.primaryContent = data.getJSONObject("primaryContent");
        this.port = data.has("port") ? data.getInt("port") : 7000;
    }


    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("address", this.address);
            json.put("port", this.port);
            json.put("primaryContent", this.primaryContent);
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
