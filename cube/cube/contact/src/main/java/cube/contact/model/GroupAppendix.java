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

package cube.contact.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import cube.contact.ContactService;
import cube.util.JSONable;

/**
 * 群组附录。
 */
public class GroupAppendix implements JSONable {

    private ContactService service;

    private Group owner;

    /**
     * 群组通告。
     */
    private String notice;

    /**
     * 成员对应的备注。
     */
    private Map<Long, String> memberRemarks;

    /**
     * 群备注。当前账号私有数据。
     */
    private String remark;

    /**
     * 是否追踪该群。
     */
    private boolean following;

    /**
     * 群的上下文。当前账号私有数据。
     */
    private JSONObject context;

    private Long commId;

    public GroupAppendix(ContactService service, Group owner, JSONObject json) throws JSONException {
        this.service = service;
        this.owner = owner;
        this.notice = json.getString("notice");

        this.memberRemarks = new HashMap<>();
        JSONArray array = json.getJSONArray("memberRemarks");
        for (int i = 0; i < array.length(); ++i) {
            JSONObject data = array.getJSONObject(i);
            Long id = data.getLong("id");
            String name = data.getString("name");
            this.memberRemarks.put(id, name);
        }

        this.remark = json.getString("remark");
        this.following = json.getBoolean("following");

        if (json.has("context")) {
            this.context = json.getJSONObject("context");
        }

        this.commId = json.getLong("commId");
    }

    public Group getOwner() {
        return this.owner;
    }

    public String getRemark() {
        return this.remark;
    }

    public boolean hasRemark() {
        return (null != this.remark && this.remark.length() > 0);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("ownerId", this.owner.getId());
            json.put("notice", this.notice);

            JSONArray array = new JSONArray();
            for (Map.Entry<Long, String> entry : this.memberRemarks.entrySet()) {
                JSONObject data = new JSONObject();
                data.put("id", entry.getKey().longValue());
                data.put("name", entry.getValue());
                array.put(data);
            }
            json.put("memberRemarks", array);

            json.put("remark", this.remark);
            json.put("following", this.following);

            if (null != this.context) {
                json.put("context", this.context);
            }

            json.put("commId", this.commId.longValue());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
