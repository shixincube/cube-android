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

import java.nio.charset.StandardCharsets;
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

    /**
     * 群组内核分类标签。
     */
    private String tag;

    /**
     * 群主 ID 。
     */
    private Long ownerId;

    /**
     * "我"是否是该群组的群主。
     */
    private boolean isOwner;

    /**
     * 创建时间。
     */
    private long creation;

    /**
     * 最近一次数据更新时间。
     */
    private long lastActive;

    /**
     * 群组状态描述。
     */
    private GroupState state;

    /**
     * 成员 ID 列表。
     */
    private List<Long> memberIdList;

    /**
     * 成员列表。
     */
    private List<Contact> memberList;

    /**
     * 群组附录。
     */
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

    /**
     * 获取群组优先显示的名称。
     *
     * @return 返回群组优先显示的名称。
     */
    public String getPriorityName() {
        if (null == this.appendix) {
            return this.name;
        }

        return this.appendix.hasRemark() ? this.appendix.getRemark() : this.name;
    }

    /**
     * 获取群主。
     *
     * @return 返回该群的群主。
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

    /**
     * 获取群主 ID 。
     *
     * @return 返回群主 ID 。
     */
    public Long getOwnerId() {
        return this.ownerId;
    }

    /**
     * 判断"我"是不是该群的群主。
     *
     * @return 如果当前签入的联系人是群主返回 {@code true} 。
     */
    public boolean isOwner() {
        return this.isOwner;
    }

    /**
     * 获取群组内核分类标签。
     *
     * <b>Non-public API</b>
     *
     * @return 返回群组内核分类标签。
     */
    public String getTag() {
        return this.tag;
    }

    /**
     * 获取群组的创建时间。
     *
     * @return 返回群组的创建时间。
     */
    public long getCreationTime() {
        return this.creation;
    }

    /**
     * 获取群组数据最近一次的更新时间。
     *
     * @return 返回群组数据最近一次的更新时间。
     */
    public long getLastActive() {
        return this.lastActive;
    }

    /**
     * 获取群组所有成员的 ID 列表。
     *
     * @return 返回群组所有成员的 ID 列表。
     */
    public List<Long> getMemberIdList() {
        return this.memberIdList;
    }

    /**
     * 获取群组状态。
     *
     * @return 返回群组状态。
     */
    public GroupState getState() {
        return this.state;
    }

    /**
     * 获取群组的附录。
     *
     * @return 返回群组的附录。
     */
    public GroupAppendix getAppendix() {
        return this.appendix;
    }

    /**
     * 当前群组的成员数量。
     *
     * @return 返回群组的成员数量。
     */
    public int numMembers() {
        return this.memberIdList.size();
    }

    /**
     * 判断指定联系人是否是该群组成员。
     *
     * @param contact 指定联系人。
     * @return 如果是该群组成员返回 {@code true} 。
     */
    public boolean isMember(Contact contact) {
        for (Long id : this.memberIdList) {
            if (id.longValue() == contact.id.longValue()) {
                return true;
            }
        }

        return false;
    }

    /**
     * <b>Non-public API</b>
     *
     * @param memberId
     */
    public void addMember(Long memberId) {
        if (!this.memberIdList.contains(memberId)) {
            this.memberIdList.add(memberId);
        }
    }

    /**
     * <b>Non-public API</b>
     *
     * @param memberId
     */
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

    /**
     * 获取按照名称排序的成员列表。
     *
     * @return 返回按照名称排序的成员列表。
     */
    public List<Contact> getOrderedMemberList() {
        if (null == this.memberList) {
            return null;
        }

        List<Contact> list = new ArrayList<>(this.memberList);
        Collections.sort(list, new NameComparator());
        return list;
    }

    /**
     * 获取群组成员列表。
     *
     * @return 返回群组成员列表。
     */
    public List<Contact> getMemberList() {
        return this.memberList;
    }

    /**
     * 获取除群主外的其他成员列表。
     *
     * @return 返回群组成员列表。
     */
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

    /**
     * 获取指定 ID 的群组成员。
     *
     * @param contactId 指定联系人 ID 。
     * @return 返回联系人实例。
     */
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

    /**
     * <b>Non-public API</b>
     *
     * @param appendix
     */
    public void setAppendix(GroupAppendix appendix) {
        this.appendix = appendix;
    }

    /**
     * <b>Non-public API</b>
     *
     * @param state
     */
    public void setState(GroupState state) {
        this.state = state;
    }

    /**
     * <b>Non-public API</b>
     *
     * @param time
     */
    public void setLastActive(long time) {
        this.lastActive = time;
        this.resetLast(time);
    }

    /**
     * <b>Non-public API</b>
     *
     * @param value
     */
    public void setIsOwner(boolean value) {
        this.isOwner = value;
    }

    /**
     * <b>Non-public API</b>
     *
     * @param contact
     */
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
     * <b>Non-public API</b>
     *
     * @return
     */
    public boolean isFilled() {
        return (null != this.memberList && this.memberList.size() == this.memberIdList.size()
            && (this.appendix.hasNotice() && null != this.appendix.getNoticeOperator()));
    }

    /**
     * 将指定群组的数据更新到当前群组实例。
     *
     * <b>Non-public API</b>
     *
     * @param source
     */
    public void update(Group source) {
        this.ownerId = source.ownerId;
        this.tag = source.tag;
        this.name = source.name;
        this.state = source.state;
        this.lastActive = source.lastActive;
    }

    @Override
    public int getMemorySize() {
        int size = super.getMemorySize();
        size += 8 * 8 + 1;

        size += this.tag.getBytes(StandardCharsets.UTF_8).length;
        size += 16 * this.memberIdList.size();

        if (null != this.memberList) {
            size += 8 * this.memberList.size();
        }

        if (null != this.appendix) {
            size += this.appendix.getMemorySize();
        }

        return size;
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
