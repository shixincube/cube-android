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

import com.shixincube.app.R;
import com.shixincube.app.util.AvatarUtils;

import cube.contact.model.Contact;
import cube.messaging.model.Conversation;
import cube.messaging.model.ConversationType;

/**
 * 会话描述。
 */
public class MessageConversation {

    public final Conversation conversation;

    /**
     * 头像图片 URL 。
     */
    private String avatarURL;

    /**
     * 内置头像的资源 ID 。
     */
    public final int avatarResourceId;

    public MessageConversation(Conversation conversation) {
        this.conversation = conversation;

        if (conversation.getType() == ConversationType.Contact) {
            Contact contact = conversation.getContact();
            this.avatarResourceId = AvatarUtils.getAvatarResource(contact);
        }
        else if (conversation.getType() == ConversationType.Group) {
            this.avatarResourceId = R.mipmap.avatar_default;
        }
        else {
            this.avatarResourceId = R.mipmap.avatar_default;
        }
    }

    public Conversation getConversation() {
        return this.conversation;
    }
}
