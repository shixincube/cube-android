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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 群组描述。包含了多个联系人的集合。
 */
public class Group extends AbstractContact implements Comparator<Group> {

    private String tag;

    private Long ownerId;

    private long creation;

    private long lastActive;

    private GroupState state;

    private List<Long> memberIdList;

    private List<Contact> memberList;

    private GroupAppendix appendix;

    public Group(Long ownerId, String name) {
        super(name);
        this.ownerId = ownerId;
        this.tag = GroupTag.Public;
        this.creation = this.getTimestamp();
        this.lastActive = this.creation;
        this.state = GroupState.Normal;
        this.memberIdList = new ArrayList<>();
    }

    public Group(JSONObject json) throws JSONException {
        super(json);
        this.tag = json.getString("tag");
        this.ownerId = json.getLong("ownerId");
        this.creation = json.getLong("creation");
        this.lastActive = json.getLong("lastActive");
        this.state = GroupState.parse(json.getInt("state"));
        this.memberIdList = new ArrayList<>();

        if (json.has("members")) {
            JSONArray array = json.getJSONArray("members");
            for (int i = 0; i < array.length(); ++i) {
                this.memberIdList.add(array.getLong(i));
            }
        }
    }

    public String getPriorityName() {
        return this.name;
    }

    public Long getOwnerId() {
        return this.ownerId;
    }

    public String getTag() {
        return this.tag;
    }

    public long getCreationTime() {
        return this.creation;
    }

    public long getLastActive() {
        return this.lastActive;
    }

    public List<Long> getMemberIdList() {
        return this.memberIdList;
    }

    public GroupState getState() {
        return this.state;
    }

    public GroupAppendix getAppendix() {
        return this.appendix;
    }

    public boolean isMember(Contact contact) {
        for (Long id : this.memberIdList) {
            if (id.longValue() == contact.id.longValue()) {
                return true;
            }
        }

        return false;
    }

    public void setAppendix(GroupAppendix appendix) {
        this.appendix = appendix;
    }

    @Override
    public int compare(Group group1, Group group2) {
        // 时间降序
        return (int) (group2.lastActive - group1.lastActive);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("tag", this.tag);
            json.put("ownerId", this.ownerId.longValue());
            json.put("creation", this.creation);
            json.put("lastActive", this.lastActive);
            json.put("state", this.state.code);

            if (!this.memberIdList.isEmpty()) {
                JSONArray array = new JSONArray();
                for (Long memberId : this.memberIdList) {
                    array.put(memberId.longValue());
                }
                json.put("members", array);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        try {
            json.put("tag", this.tag);
            json.put("ownerId", this.ownerId.longValue());
            json.put("creation", this.creation);
            json.put("lastActive", this.lastActive);
            json.put("state", this.state.code);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
