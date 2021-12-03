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

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import cube.core.model.Entity;

/**
 * 联系人分区参与者。
 */
public class ContactZoneParticipant extends Entity {

    private ContactZoneParticipantType type;

    private ContactZoneParticipantState state;

    private Long inviterId;

    private String postscript;

    private Contact inviter;

    /**
     * 当前登录的联系人是不是邀请人。
     * 如果是表示"我"发起的邀请，如果不是表示是对方发起的邀请。
     */
    private boolean isInviter;

    private Contact contact;

    private Group group;

    public ContactZoneParticipant(Long id, long timestamp, ContactZoneParticipantType type, ContactZoneParticipantState state, Long inviterId, String postscript) {
        super(id, timestamp);
        this.type = type;
        this.state = state;
        this.inviterId = inviterId;
        this.postscript = postscript;
    }

    public ContactZoneParticipant(JSONObject json) throws JSONException {
        super(json);
        this.type = ContactZoneParticipantType.parse(json.getInt("type"));
        this.state = ContactZoneParticipantState.parse(json.getInt("state"));
        this.inviterId = json.getLong("inviterId");
        this.postscript = json.has("postscript") ? json.getString("postscript") : "";
    }

    /**
     * 获取参与人对应的联系人实例。
     *
     * @return 返回参与人对应的联系人实例。
     */
    public Contact getContact() {
        return this.contact;
    }

    /**
     * 获取参与人对应的群组实例。
     *
     * @return 返回参与人对应的群组实例。
     */
    public Group getGroup() {
        return this.group;
    }

    /**
     * 获取类型。
     *
     * @return 返回参与人类型。
     * @see ContactZoneParticipantType
     */
    public ContactZoneParticipantType getType() {
        return this.type;
    }

    /**
     * 获取参与人状态。
     *
     * @return 返回参与人状态。
     * @see ContactZoneParticipantState
     */
    public ContactZoneParticipantState getState() {
        return this.state;
    }

    /**
     * 获取邀请人。
     *
     * @return 返回该参与人的邀请人。
     */
    public Contact getInviter() {
        return this.inviter;
    }

    /**
     * 当前签入的联系人"我"是否是该参与人的邀请人。
     *
     * @return 如果该参与人的邀请人是"我"返回 {@code true} 。
     */
    public boolean isInviter() {
        return this.isInviter;
    }

    /**
     * 返回参与人的 ID 。
     *
     * @return 返回参与人的 ID 。
     */
    public Long getInviterId() {
        return this.inviterId;
    }

    /**
     * 获取参与人的附言数据。
     *
     * @return 返回参与人的附言数据。
     */
    public String getPostscript() {
        return this.postscript;
    }

    /**
     * <b>Non-public API</b>
     *
     * @param contact
     */
    public void setContact(Contact contact) {
        this.contact = contact;
    }

    /**
     * <b>Non-public API</b>
     *
     * @param group
     */
    public void setGroup(Group group) {
        this.group = group;
    }

    /**
     * <b>Non-public API</b>
     *
     * @param state
     */
    public void setState(ContactZoneParticipantState state) {
        this.state = state;
    }

    /**
     * <b>Non-public API</b>
     *
     * @param inviter
     * @param isInviter
     */
    public void setInviter(Contact inviter, boolean isInviter) {
        this.inviter = inviter;
        this.isInviter = isInviter;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (null != object && object instanceof ContactZoneParticipant) {
            ContactZoneParticipant other = (ContactZoneParticipant) object;
            return other.id.longValue() == this.id.longValue();
        }

        return false;
    }

    @Override
    public int getMemorySize() {
        int size = super.getMemorySize();
        size += 8 * 8;
        size += this.postscript.getBytes(StandardCharsets.UTF_8).length;
        return size;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = this.toCompactJSON();
        try {
            json.put("postscript", this.postscript);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        try {
            json.put("type", this.type.code);
            json.put("state", this.state.code);
            json.put("inviterId", this.inviterId.longValue());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
