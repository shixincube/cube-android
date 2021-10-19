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

package cube.contact.model;

import org.json.JSONException;
import org.json.JSONObject;

import cube.core.model.Entity;

/**
 * 抽象联系人。
 */
public class AbstractContact extends Entity {

    /**
     * 联系人名称。
     */
    public final String name;

    /**
     * 联系人所属的域。
     */
    public final String domain;

    /**
     * 自定义数据，为应用程序提供关联其他数据对象的属性。
     */
    public Object customData = null;

    public AbstractContact(Long id, String name, String domain) {
        super(id);
        this.name = name;
        this.domain = domain;
    }

    public AbstractContact(JSONObject json) throws JSONException {
        super(json);
        this.name = json.getString("name");
        this.domain = json.getString("domain");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("name", this.name);
            json.put("domain", this.domain);
        } catch (JSONException e) {
            // Nothing
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        try {
            json.put("name", this.name);
            json.put("domain", this.domain);
        } catch (JSONException e) {
            // Nothing
        }
        return json;
    }
}
