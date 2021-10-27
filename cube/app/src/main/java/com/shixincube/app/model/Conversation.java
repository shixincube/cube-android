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

package com.shixincube.app.model;

import com.shixincube.app.manager.AccountHelper;

import org.json.JSONObject;

import java.util.Date;

import cube.contact.model.Contact;
import cube.contact.model.Group;
import cube.messaging.model.Message;
import cube.util.JSONable;

/**
 * 会话描述。
 */
public class Conversation implements JSONable {

    /**
     * 会话类型。
     */
    private ConversationType type;

    /**
     * 消息提示类型。
     */
    private MessageRemindType remindType;

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
    private Long id;

    /**
     * 会话显示的名称。
     */
    private String displayName;

    /**
     * 头像图片名称。
     */
    private String avatarName;

    /**
     * 头像图片 URL 。
     */
    private String avatarURL;

    /**
     * 内置头像的资源 ID 。
     */
    private int avatarResourceId;

    /**
     * 日期。
     */
    private Date date;

    /**
     * 显示的内容。
     */
    private String contentText;

    /**
     * 未读数量。
     */
    private int unread;

    public Conversation(Message message) {
        if (message.isFromGroup()) {
            this.type = ConversationType.Group;
            this.id = message.getSource();
            this.group = message.getSourceGroup();
            // TODO
        }
        else {
            this.type = ConversationType.Contact;
            this.id = message.getPartner().id;
            this.contact = message.getPartner();
            this.displayName = this.contact.getPriorityName();
            this.avatarName = (null != this.contact.getContext()) ?
                    Account.getAvatar(this.contact.getContext()) : Account.DefaultAvatarName;
        }

        // 头像处理
        this.avatarResourceId = AccountHelper.explainAvatarForResource(this.avatarName);

        this.remindType = MessageRemindType.Normal;

        // 时间
        this.date = new Date(message.getRemoteTimestamp());

        // 信息摘要
        this.contentText = message.getSummary();
    }

    public Long getId() {
        return this.id;
    }

    public ConversationType getType() {
        return this.type;
    }

    public MessageRemindType getRemindType() {
        return this.remindType;
    }

    public Contact getContact() {
        return this.contact;
    }

    public Group getGroup() {
        return this.group;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getAvatarResourceId() {
        return this.avatarResourceId;
    }

    public Date getDate() {
        return this.date;
    }

    public String getContentText() {
        return this.contentText;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        return json;
    }
}
