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

import cube.contact.model.Contact;
import cube.contact.model.Group;

/**
 * 空消息，仅用于数据管理的消息。
 */
public class NullMessage extends Message {

    public NullMessage(Contact sender, Contact receiver) {
        this.assign(sender.id, receiver.id, 0);
        this.setSender(sender);
        this.setReceiver(receiver);
        this.setRemoteTS(this.getTimestamp());
        this.scope = MessageScope.Private;
        this.summary = "";
    }

    public NullMessage(Contact sender, Group group) {
        this.assign(sender.id, 0, group.id);
        this.setSender(sender);
        this.setSourceGroup(group);
        this.setRemoteTS(this.getTimestamp());
        this.scope = MessageScope.Private;
        this.summary = "";
    }

    public NullMessage(Long senderId, Long receiverId, Long sourceGroupId) {
        super();
        this.assign(senderId, receiverId, sourceGroupId);
        this.setRemoteTS(this.getTimestamp());
        this.scope = MessageScope.Private;
        this.summary = "";
    }

    @Override
    public boolean isEmpty() {
        return true;
    }
}
