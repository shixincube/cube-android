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

package cube.auth;

import org.json.JSONException;
import org.json.JSONObject;

import cube.util.JSONable;

/**
 * 授权令牌。
 */
public class AuthToken implements JSONable {

    /**
     * 授权码。
     */
    public final String code;

    /**
     * 工作的域。
     */
    public final String domain;

    /**
     * 工作的 App Key 。
     */
    public final String appKey;

    /**
     * 令牌绑定的联系人 ID 。
     */
    public long cid;

    /**
     * 令牌发布时间戳。
     */
    public final long issues;

    /**
     * 令牌过期时间戳。
     */
    public final long expiry;

    /**
     * 引擎的主描述内容。
     */
    public final PrimaryDescription description;

    /**
     * 构造函数。
     *
     * @param json 使用 JSON 结构初始化数据。
     * @throws JSONException 格式错误时抛出该异常。
     */
    public AuthToken(JSONObject json) throws JSONException {
        this.code = json.getString("code");
        this.domain = json.getString("domain");
        this.appKey = json.getString("appKey");
        this.cid = json.getLong("cid");
        this.issues = json.getLong("issues");
        this.expiry = json.getLong("expiry");
        this.description = new PrimaryDescription(json.getJSONObject("description"));
    }

    /**
     * 是否有效。
     *
     * @return 如果有效返回 {@code true} 。
     */
    public boolean isValid() {
        long now = System.currentTimeMillis();
        return (now + 60000) < this.expiry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("code", this.code);
            json.put("domain", this.domain);
            json.put("appKey", this.appKey);
            json.put("cid", this.cid);
            json.put("issues", this.issues);
            json.put("expiry", this.expiry);
            json.put("description", this.description.toJSON());
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
        return this.toJSON();
    }
}
