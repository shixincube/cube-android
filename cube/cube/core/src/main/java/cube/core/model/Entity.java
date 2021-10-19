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

package cube.core.model;

import org.json.JSONException;
import org.json.JSONObject;

import cell.util.Utils;
import cube.util.JSONable;

/**
 * 信息实体对象。所有实体对象的基类。
 */
public class Entity implements JSONable {

    private final static long LIFECYCLE_IN_MSEC = 7L * 24L * 60L * 60L * 1000L;

    /**
     * 实体 ID 。
     */
    public final Long id;

    /**
     * 实体创建时的时间戳。
     */
    protected long timestamp;

    /**
     * 实体的到期时间。
     */
    protected long expiry;

    /**
     * 关联上下文数据。
     */
    protected JSONObject context;

    public Entity() {
        this.id = Utils.generateUnsignedSerialNumber();
        this.timestamp = System.currentTimeMillis();
        this.expiry = this.timestamp + LIFECYCLE_IN_MSEC;
    }

    public Entity(Long id) {
        this.id = id;
        this.timestamp = System.currentTimeMillis();
        this.expiry = this.timestamp + LIFECYCLE_IN_MSEC;
    }

    public Entity(Long id, long timestamp) {
        this.id = id;
        this.timestamp = timestamp;
        this.expiry = this.timestamp + LIFECYCLE_IN_MSEC;
    }

    public Entity(JSONObject json) throws JSONException {
        this.id = json.getLong("id");

        if (json.has("timestamp")) {
            this.timestamp = json.getLong("timestamp");
        }
        else {
            this.timestamp = System.currentTimeMillis();
        }

        if (json.has("expiry")) {
            this.expiry = json.getLong("expiry");
        }
        else {
            this.expiry = this.timestamp + LIFECYCLE_IN_MSEC;
        }

        if (json.has("context")) {
            this.context = json.getJSONObject("context");
        }
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public long getExpiry() {
        return this.expiry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id.longValue());
            json.put("timestamp", this.timestamp);
            json.put("expiry", this.expiry);

            if (null != this.context) {
                json.put("context", this.context);
            }
        } catch (JSONException e) {
            // Nothing
        }
        return json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id.longValue());

            if (null != this.context) {
                json.put("context", this.context);
            }
        } catch (JSONException e) {
            // Nothing
        }
        return json;
    }
}
