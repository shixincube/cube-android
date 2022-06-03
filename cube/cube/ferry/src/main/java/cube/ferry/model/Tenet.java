/*
 * This source file is part of Cube.
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020-2022 Cube Team.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
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

import cube.util.JSONable;

/**
 * 信条。
 */
public abstract class Tenet implements JSONable {

    private final String port;

    private String domain;

    private long timestamp;

    public Tenet(String port, String domain, long timestamp) {
        this.port = port;
        this.domain = domain;
        this.timestamp = timestamp;
    }

    public Tenet(JSONObject json) throws JSONException {
        this.port = json.getString("port");
        this.domain = json.getString("domain");
        this.timestamp = json.getLong("timestamp");
    }

    public String getPort() {
        return this.port;
    }

    public String getDomain() {
        return this.domain;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("port", this.port);
            json.put("domain", this.domain);
            json.put("timestamp", this.timestamp);
        } catch (JSONException e) {
            // Nothing
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    /**
     * 提取 JSON 里的 Port 名。
     *
     * @param json
     * @return
     * @throws JSONException
     */
    public static String extractPort(JSONObject json) throws JSONException {
        return json.getString("port");
    }
}
