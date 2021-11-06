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

package cube.messaging.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import cube.contact.model.Contact;
import cube.contact.model.Group;
import cube.core.model.Entity;

/**
 * 会话描述。
 */
public class Conversation extends Entity {

    /**
     * 会话类型。
     */
    private ConversationType type;

    /**
     * 会话状态。
     */
    private ConversationState state;

    /**
     * 与会话相关的关键实体的 ID 。
     */
    private Long pivotalId;

    /**
     * 关联的联系人。
     */
    private Contact contact;

    /**
     * 关联的群组。
     */
    private Group group;

    /**
     * 该会话联系人或群组的 ID 。
     */
    private Message recentMessage;

    /**
     * 会话提醒类型。
     */
    private ConversationReminded reminded;

    /**
     * 未读消息数量。
     */
    private int unreadCount;

    /**
     * 头像图片名称。
     */
    private String avatarName;

    /**
     * 头像图片 URL 。
     */
    private String avatarURL;

    /**
     * 构造函数。
     *
     * @param json
     * @throws JSONException
     */
    public Conversation(JSONObject json) throws JSONException {
        super(json);
        this.type = ConversationType.parse(json.getInt("type"));
        this.state = ConversationState.parse(json.getInt("state"));
        this.reminded = ConversationReminded.parse(json.getInt("remind"));
        this.pivotalId = json.getLong("pivotal");
        this.unreadCount = json.getInt("unread");

        this.recentMessage = new Message(null, json.getJSONObject("recentMessage"));

        if (json.has("avatarName")) {
            this.avatarName = json.getString("avatarName");
        }
        if (json.has("avatarURL")) {
            this.avatarURL = json.getString("avatarURL");
        }
    }

    /**
     * 构造函数。
     *
     * @param id
     * @param timestamp
     * @param type
     * @param state
     * @param pivotalId
     * @param recentMessage
     * @param reminded
     */
    public Conversation(Long id, long timestamp, ConversationType type, ConversationState state, Long pivotalId, Message recentMessage, ConversationReminded reminded, int unreadCount) {
        super(id, timestamp);
        this.type = type;
        this.state = state;
        this.pivotalId = pivotalId;
        this.recentMessage = recentMessage;
        this.reminded = reminded;
        this.unreadCount = unreadCount;
    }

    /**
     * 构造函数。
     *
     * @param contact
     * @param reminded
     */
    public Conversation(Contact contact, ConversationReminded reminded) {
        super(contact.id);
        this.pivotalId = contact.id;
        this.contact = contact;
        this.type = ConversationType.Contact;
        this.reminded = reminded;
        this.state = ConversationState.Normal;
        this.recentMessage = new NullMessage();
        this.unreadCount = 0;
    }

    /**
     * 获取会话类型。
     *
     * @return
     */
    public ConversationType getType() {
        return this.type;
    }

    public ConversationState getState() {
        return this.state;
    }

    public Long getPivotalId() {
        return this.pivotalId;
    }

    public Contact getContact() {
        return this.contact;
    }

    public Group getGroup() {
        return this.group;
    }

    public Message getRecentMessage() {
        return this.recentMessage;
    }

    public ConversationReminded getReminded() {
        return this.reminded;
    }

    public int getUnreadCount() {
        return this.unreadCount;
    }

    public String getAvatarName() {
        return this.avatarName;
    }

    public String getAvatarURL() {
        return this.avatarURL;
    }

    /**
     * 获取显示名。
     *
     * @return
     */
    public String getDisplayName() {
        if (null != this.contact) {
            return this.contact.getPriorityName();
        }
        else if (null != this.group) {
            return this.group.getPriorityName();
        }
        else {
            return "";
        }
    }

    /**
     * 获取最近的更新日期。
     *
     * @return
     */
    public Date getDate() {
        return new Date(this.timestamp);
    }

    /**
     * 获取最近的摘要。
     *
     * @return
     */
    public String getRecentSummary() {
        return this.recentMessage.getSummary();
    }

    public void setRecentMessage(Message recentMessage) {
        this.recentMessage = recentMessage;
        if (recentMessage.getRemoteTimestamp() > this.timestamp) {
            this.timestamp = recentMessage.getRemoteTimestamp();
        }
    }

    public void setPivotal(Contact contact) {
        this.contact = contact;
    }

    public void setPivotal(Group group) {
        this.group = group;
    }

    public void setUnreadCount(int count) {
        this.unreadCount = count;
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof Conversation) {
            Conversation other = (Conversation) object;
            if (other.id.longValue() == this.id.longValue()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        return json;
    }
}
