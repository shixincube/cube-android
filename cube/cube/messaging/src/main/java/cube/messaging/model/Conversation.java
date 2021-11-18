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

import cube.auth.AuthService;
import cube.contact.model.Contact;
import cube.contact.model.Group;
import cube.contact.model.Self;
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

        if (json.has("recentMessage")) {
            this.recentMessage = new Message(null, json.getJSONObject("recentMessage"));
        }
        else {
            if (this.type == ConversationType.Contact) {
                this.recentMessage = new NullMessage(this.pivotalId, this.pivotalId, 0L);
            }
            else {
                this.recentMessage = new NullMessage(this.pivotalId, 0L, this.pivotalId);
            }
        }

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
     * @param reminded
     * @param unreadCount
     * @param recentMessage
     */
    public Conversation(Long id, long timestamp, ConversationType type, ConversationState state, Long pivotalId, ConversationReminded reminded, int unreadCount, Message recentMessage) {
        super(id, timestamp);
        this.type = type;
        this.state = state;
        this.pivotalId = pivotalId;
        this.reminded = reminded;
        this.unreadCount = unreadCount;
        this.recentMessage = recentMessage;
    }

    /**
     * 构造函数。
     *
     * @param contact
     * @param reminded
     */
    public Conversation(Self self, Contact contact, ConversationReminded reminded) {
        super(contact.id);
        this.pivotalId = contact.id;
        this.contact = contact;
        this.type = ConversationType.Contact;
        this.reminded = reminded;
        this.state = ConversationState.Normal;
        this.recentMessage = new NullMessage(self, contact);
        this.unreadCount = 0;
    }

    /**
     * 获取会话类型。
     *
     * @return 返回会话类型。
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

    /**
     * 获取会话对应的联系人。
     *
     * @return 返回会话对应的联系人。
     */
    public Contact getContact() {
        return this.contact;
    }

    /**
     * 获取会话对应的群组。
     *
     * @return 返回会话对应的群组。
     */
    public Group getGroup() {
        return this.group;
    }

    /**
     * 获取最近的消息。
     *
     * @return 返回最近的消息实例。
     */
    public Message getRecentMessage() {
        return this.recentMessage;
    }

    /**
     * 获取会话提醒类型。
     *
     * @return 返回会话提醒类型。
     */
    public ConversationReminded getReminded() {
        return this.reminded;
    }

    /**
     * 获取未读消息数量。
     *
     * @return 返回未读消息数量。
     */
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
     * @return 返回会话的优先显示名。
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
     * @return 返回最近的更新日期。
     */
    public Date getDate() {
        return new Date(this.timestamp);
    }

    /**
     * 获取最近的摘要。
     *
     * @return 获取最近的摘要。
     */
    public String getRecentSummary() {
        return this.recentMessage.getSummary();
    }

    /**
     * 判断会话是否是已关注的会话。
     *
     * @return 如果是已关注的会话，返回 {@code true} 。
     */
    public boolean focused() {
        return this.state == ConversationState.Important;
    }

    public void setRecentMessage(Message recentMessage) {
        this.recentMessage = recentMessage;
        if (recentMessage.getRemoteTimestamp() > this.timestamp) {
            this.timestamp = recentMessage.getRemoteTimestamp();
        }
    }

    @Override
    public void setTimestamp(long timestamp) {
        if (timestamp > this.timestamp) {
            this.timestamp = timestamp;
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

    public void setState(ConversationState state) {
        this.state = state;
    }

    public void setReminded(ConversationReminded reminded) {
        this.reminded = reminded;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

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
        JSONObject json = super.toCompactJSON();
        try {
            json.put("domain", AuthService.getDomain());
            json.put("type", this.type.code);
            json.put("state", this.state.code);
            json.put("remind", this.reminded.code);
            json.put("pivotal", this.pivotalId.longValue());
            json.put("unread", this.unreadCount);

            if (null != this.recentMessage) {
                json.put("recentMessage", this.recentMessage.toJSON());
            }

            if (null != this.avatarName) {
                json.put("avatarName", this.avatarName);
            }
            if (null != this.avatarURL) {
                json.put("avatarURL", this.avatarURL);
            }
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
