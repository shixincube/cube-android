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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 属性。
 */
public abstract class Property {

    /**
     * 上下文数据。
     */
    protected JSONObject context;

    public Property() {
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

    /**
     * 是否包含指定名称的属性值。
     *
     * @param name 指定名称。
     * @return 如果包含指定名称的值返回 {@code true} 。
     */
    public boolean has(String name) {
        return null != this.context && this.context.has(name);
    }

    public void set(String name, String value) {
        if (null == this.context) {
            this.context = new JSONObject();
        }

        try {
            this.context.put(name, value);
        } catch (JSONException e) {
            // Nothing
        }
    }

    public void set(String name, int value) {
        if (null == this.context) {
            this.context = new JSONObject();
        }

        try {
            this.context.put(name, value);
        } catch (JSONException e) {
            // Nothing
        }
    }

    public void set(String name, long value) {
        if (null == this.context) {
            this.context = new JSONObject();
        }

        try {
            this.context.put(name, value);
        } catch (JSONException e) {
            // Nothing
        }
    }

    public void set(String name, boolean value) {
        if (null == this.context) {
            this.context = new JSONObject();
        }

        try {
            this.context.put(name, value);
        } catch (JSONException e) {
            // Nothing
        }
    }

    public void set(String name, JSONObject value) {
        if (null == this.context) {
            this.context = new JSONObject();
        }

        try {
            this.context.put(name, value);
        } catch (JSONException e) {
            // Nothing
        }
    }

    public void set(String name, JSONArray value) {
        if (null == this.context) {
            this.context = new JSONObject();
        }

        try {
            this.context.put(name, value);
        } catch (JSONException e) {
            // Nothing
        }
    }

    public String getString(String name) {
        try {
            return this.has(name) ? this.context.getString(name) : null;
        } catch (JSONException e) {
            // Nothing
        }
        return null;
    }

    public int getInt(String name, int defaultValue) {
        try {
            return this.has(name) ? this.context.getInt(name) : defaultValue;
        } catch (JSONException e) {
            // Nothing
        }
        return defaultValue;
    }

    public long getLong(String name, long defaultValue) {
        try {
            return this.has(name) ? this.context.getLong(name) : defaultValue;
        } catch (JSONException e) {
            // Nothing
        }
        return defaultValue;
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        try {
            return this.has(name) ? this.context.getBoolean(name) : defaultValue;
        } catch (JSONException e) {
            // Nothing
        }
        return defaultValue;
    }

    public JSONObject getJSONObject(String name) {
        try {
            return this.has(name) ? this.context.getJSONObject(name) : null;
        } catch (JSONException e) {
            // Nothing
        }
        return null;
    }

    public JSONArray getJSONArray(String name) {
        try {
            return this.has(name) ? this.context.getJSONArray(name) : null;
        } catch (JSONException e) {
            // Nothing
        }
        return null;
    }
}
