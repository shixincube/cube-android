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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 群组描述。包含了多个联系人的集合。
 */
public class Group extends AbstractContact implements Comparator<Group> {

    private String tag;

    private Long ownerId;

    /**
     * "我"是否是该群组的群主。
     */
    private boolean isOwner;

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
        this.memberIdList.add(ownerId);
    }

    public Group(Long id, String name, Long ownerId, String tag,
                 long creationTime, long lastActiveTime, GroupState state) {
        super(id, name);
        this.ownerId = ownerId;
        this.tag = tag;
        this.creation = creationTime;
        this.lastActive = lastActiveTime;
        this.state = state;
        this.memberIdList = new ArrayList<>();
        this.memberIdList.add(ownerId);
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
        if (null == this.appendix) {
            return this.name;
        }

        return this.appendix.hasRemark() ? this.appendix.getRemark() : this.name;
    }

    /**
     * 获取群主。
     *
     * @return
     */
    public Contact getOwner() {
        if (null == this.memberList) {
            return null;
        }

        for (Contact contact : this.memberList) {
            if (contact.id.equals(this.ownerId)) {
                return contact;
            }
        }

        return null;
    }

    public Long getOwnerId() {
        return this.ownerId;
    }

    public boolean isOwner() {
        return this.isOwner;
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

    public int numMembers() {
        return this.memberIdList.size();
    }

    public boolean isMember(Contact contact) {
        for (Long id : this.memberIdList) {
            if (id.longValue() == contact.id.longValue()) {
                return true;
            }
        }

        return false;
    }

    public void addMember(Long memberId) {
        if (!this.memberIdList.contains(memberId)) {
            this.memberIdList.add(memberId);
        }
    }

    public void removeMember(Long memberId) {
        this.memberIdList.remove(memberId);
        
        if (null != this.memberList) {
            for (int i = 0; i < this.memberList.size(); ++i) {
                Contact contact = this.memberList.get(i);
                if (contact.id.equals(memberId)) {
                    this.memberList.remove(i);
                    break;
                }
            }
        }
    }

    public List<Contact> getOrderedMemberList() {
        if (null == this.memberList) {
            return null;
        }

        List<Contact> list = new ArrayList<>(this.memberList);
        Collections.sort(list, new NameComparator());
        return list;
    }

    public List<Contact> getMemberList() {
        return this.memberList;
    }

    public List<Contact> getMemberListWithoutOwner() {
        List<Contact> list = new ArrayList<>(getMemberList());
        for (int i = 0; i < list.size(); ++i) {
            Contact contact = list.get(i);
            if (contact.id.longValue() == this.ownerId.longValue()) {
                list.remove(i);
                break;
            }
        }
        return list;
    }

    public Contact getMember(Long contactId) {
        if (null == this.memberList) {
            return null;
        }

        for (Contact contact : this.memberList) {
            if (contact.id.equals(contactId)) {
                return contact;
            }
        }

        return null;
    }

    public void setAppendix(GroupAppendix appendix) {
        this.appendix = appendix;
    }

    public void setState(GroupState state) {
        this.state = state;
    }

    public void setLastActive(long time) {
        this.lastActive = time;
        this.resetLast(time);
    }

    public void setIsOwner(boolean value) {
        this.isOwner = value;
    }

    public void updateMember(Contact contact) {
        if (null == this.memberList) {
            this.memberList = new ArrayList<>();
            this.memberList.add(contact);
            return;
        }

        int index = this.memberList.indexOf(contact);
        if (index >= 0) {
            this.memberList.set(index, contact);
        }
        else {
            this.memberList.add(contact);
        }
    }

    /**
     * 是否已填充数据。
     *
     * @return
     */
    public boolean isFilled() {
        return (null != this.memberList && this.memberList.size() == this.memberIdList.size()
            && (this.appendix.hasNotice() && null != this.appendix.getNoticeOperator()));
    }

    public void update(Group source) {
        this.ownerId = source.ownerId;
        this.tag = source.tag;
        this.name = source.name;
        this.state = source.state;
        this.lastActive = source.lastActive;
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

    protected class NameComparator implements Comparator<Contact> {

        private Collator collator;

        public NameComparator() {
            this.collator = Collator.getInstance(Locale.CHINESE);
        }

        @Override
        public int compare(Contact contact1, Contact contact2) {
            return this.collator.compare(contact1.getPriorityName(),
                    contact2.getPriorityName());
        }
    }
}
