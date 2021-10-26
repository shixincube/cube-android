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

import cube.auth.AuthService;
import cube.contact.model.Contact;
import cube.contact.model.Group;
import cube.core.model.Entity;
import cube.messaging.MessagingService;

/**
 * 消息实体。
 */
public class Message extends Entity {

    public final String domain;

    private long from;

    private Contact sender;

    private long to;

    private Contact receiver;

    private long source;

    private Group sourceGroup;

    private long owner;

    private long localTS;

    private long remoteTS;

    private JSONObject payload;

    private String summary;

    private MessageState state;

    private boolean selfTyper;

    private int scope;

    public Message(JSONObject payload) {
        super();
        this.domain = AuthService.getDomain();
        this.payload = payload;
        this.localTS = System.currentTimeMillis();
        this.remoteTS = 0;
        this.from = 0;
        this.to = 0;
        this.source = 0;
        this.owner = 0;
        this.state = MessageState.Unknown;
        this.scope = MessageScope.Unlimited;
    }

    public Message(JSONObject json, MessagingService service) throws JSONException {
        super(json);
        this.domain = json.getString("domain");
    }

    public Contact getPartner() {
        return null;
    }
}
