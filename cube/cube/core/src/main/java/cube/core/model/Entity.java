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
public class Entity implements TimeSortable, JSONable {

    public final static long LIFECYCLE_IN_MSEC = 7L * 24L * 60L * 60L * 1000L;

    /**
     * 实体 ID 。
     */
    public final Long id;

    /**
     * 数据时间戳。
     */
    protected long timestamp;

    /**
     * 该实体数据上次更新的时间戳。
     */
    protected long last;

    /**
     * 数据到期时间。
     */
    protected long expiry;

    /**
     * 关联上下文数据。
     */
    protected JSONObject context;

    /**
     * 实体创建时的时间戳。
     */
    public final long entityCreation;

    public Entity() {
        this.id = Utils.generateUnsignedSerialNumber();
        this.entityCreation = System.currentTimeMillis();
        this.timestamp = this.entityCreation;
        this.last = this.timestamp;
        this.expiry = this.last + LIFECYCLE_IN_MSEC;
    }

    public Entity(Long id) {
        this.id = id;
        this.entityCreation = System.currentTimeMillis();
        this.timestamp = this.entityCreation;
        this.last = this.timestamp;
        this.expiry = this.last + LIFECYCLE_IN_MSEC;
    }

    public Entity(Long id, long timestamp) {
        this.id = id;
        this.entityCreation = System.currentTimeMillis();
        this.timestamp = timestamp;
        this.last = timestamp;
        this.expiry = this.last + LIFECYCLE_IN_MSEC;
    }

    public Entity(JSONObject json) throws JSONException {
        this.entityCreation = System.currentTimeMillis();

        this.id = json.getLong("id");

        if (json.has("timestamp"))
            this.timestamp = json.getLong("timestamp");
        else
            this.timestamp = this.entityCreation;

        if (json.has("last"))
            this.last = json.getLong("last");
        else
            this.last = this.entityCreation;

        if (json.has("expiry"))
            this.expiry = json.getLong("expiry");
        else
            this.expiry = this.last + LIFECYCLE_IN_MSEC;

        if (json.has("context")) {
            this.context = json.getJSONObject("context");
        }
    }

    /**
     * 获取实体 ID 。
     *
     * @return
     */
    public Long getId() {
        return this.id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSortableTime() {
        return this.timestamp;
    }

    /**
     * 获取数据的时间戳。
     *
     * @return
     */
    public long getTimestamp() {
        return this.timestamp;
    }

    /**
     * 获取最近一次更新该实体数据的时间。
     *
     * @return
     */
    public long getLast() {
        return this.last;
    }

    /**
     * 获取该实体数据的到期时间。
     *
     * @return
     */
    public long getExpiry() {
        return this.expiry;
    }

    public void resetLast(long time) {
        this.last = time;
        this.expiry = time + LIFECYCLE_IN_MSEC;
    }

    public void resetExpiry(long expiry, long last) {
        this.last = last;
        this.expiry = expiry;
    }

    /**
     * 实体数据是否在有效期内。
     *
     * @return 如果数据在有效期内返回 {@code true} ，否则返回 {@code false} 。
     */
    public boolean isValid() {
        return this.expiry > System.currentTimeMillis();
    }

    public void setContext(JSONObject context) {
        this.context = context;
    }

    public JSONObject getContext() {
        return this.context;
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
            json.put("last", this.last);
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
