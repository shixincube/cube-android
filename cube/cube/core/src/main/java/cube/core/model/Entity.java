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

import java.nio.charset.StandardCharsets;

import cell.util.Utils;
import cube.util.JSONable;

/**
 * 信息实体对象。所有实体对象的基类。
 */
public class Entity implements JSONable, Cacheable, TimeSortable {

    public final static long LIFESPAN_IN_MSEC = 24 * 60 * 60 * 1000;

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
     * 上下文数据。
     */
    protected JSONObject context;

    /**
     * @private
     * 实体创建时的时间戳。
     */
    public final long entityCreation;

    /**
     * @private
     * 实体内存寿命。
     */
    public long entityLifeExpiry;

    /**
     * 构造函数。
     */
    public Entity() {
        this.id = Utils.generateUnsignedSerialNumber();
        this.entityCreation = System.currentTimeMillis();
        this.entityLifeExpiry = this.entityCreation + 5L * 60L * 1000L;
        this.timestamp = this.entityCreation;
        this.last = this.timestamp;
        this.expiry = this.last + LIFESPAN_IN_MSEC;
    }

    /**
     * 构造函数。
     *
     * @param id 指定实体 ID 。
     */
    public Entity(Long id) {
        this.id = id;
        this.entityCreation = System.currentTimeMillis();
        this.entityLifeExpiry = this.entityCreation + 5 * 60 * 1000;
        this.timestamp = this.entityCreation;
        this.last = this.timestamp;
        this.expiry = this.last + LIFESPAN_IN_MSEC;
    }

    /**
     * 构造函数。
     *
     * @param id 指定实体 ID 。
     * @param timestamp 指定时间戳。
     */
    public Entity(Long id, long timestamp) {
        this.id = id;
        this.entityCreation = System.currentTimeMillis();
        this.entityLifeExpiry = this.entityCreation + 5 * 60 * 1000;
        this.timestamp = timestamp;
        this.last = timestamp;
        this.expiry = this.last + LIFESPAN_IN_MSEC;
    }

    /**
     * 构造函数。
     *
     * @param json 指定 JSON 数据。
     * @throws JSONException 如果 JSON 数据项错误则抛出该异常。
     */
    public Entity(JSONObject json) throws JSONException {
        this.entityCreation = System.currentTimeMillis();
        this.entityLifeExpiry = this.entityCreation + 5 * 60 * 1000;

        if (json.has("id"))
            this.id = json.getLong("id");
        else
            this.id = Utils.generateUnsignedSerialNumber();

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
            this.expiry = this.last + LIFESPAN_IN_MSEC;

        if (json.has("context")) {
            this.context = json.getJSONObject("context");
        }
    }

    /**
     * 获取实体 ID 。
     *
     * @return 返回实体 ID 。
     */
    public final Long getId() {
        return this.id;
    }

    /**
     * 获取数据的时间戳。
     *
     * @return 返回数据的时间戳。
     */
    public long getTimestamp() {
        return this.timestamp;
    }

    /**
     * 获取最近一次更新该实体数据的时间。
     *
     * @return 返回最近一次更新该实体数据的时间。
     */
    public long getLast() {
        return this.last;
    }

    /**
     * 获取该实体数据的到期时间。
     *
     * @return 返回该实体数据的到期时间。
     */
    public long getExpiry() {
        return this.expiry;
    }

    /**
     * 重置最近一次更新时间。
     *
     * @param time 指定时间戳。
     */
    public void resetLast(long time) {
        this.last = time;
        this.expiry = time + LIFESPAN_IN_MSEC;
    }

    /**
     * 重置有效期和最近一次更新时间。
     *
     * @param expiry 指定有效期。
     * @param last 指定最近一次更新时间。
     */
    public void resetExpiry(long expiry, long last) {
        this.last = last;
        this.expiry = expiry;
    }

    /**
     * 设置时间戳。
     *
     * @param timestamp 指定时间戳。
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * 实体数据是否在有效期内。
     *
     * @return 如果数据在有效期内返回 {@code true} ，否则返回 {@code false} 。
     */
    public boolean isValid() {
        return this.expiry > System.currentTimeMillis();
    }

    /**
     * 设置上下文数据。
     *
     * @param context 指定上下文数据。
     */
    public void setContext(JSONObject context) {
        this.context = context;
    }

    /**
     * 获取上下文数据。
     *
     * @return 返回上下文数据。
     */
    public JSONObject getContext() {
        return this.context;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (null != object && object instanceof Entity) {
            Entity other = (Entity) object;
            return other.id.equals(this.id);
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMemorySize() {
        // 对象头 + Long 引用 + long * 5 + JSONObject 引用
        int base = 8 + 16 + 8 * 5 + 8;
        if (null == this.context) {
            return base;
        }

        String string = this.context.toString();
        return base + string.getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSortableTime() {
        return this.entityLifeExpiry;
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
            json.put("timestamp", this.timestamp);

            if (null != this.context) {
                json.put("context", this.context);
            }
        } catch (JSONException e) {
            // Nothing
        }
        return json;
    }
}
