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

    protected String summary;

    private MessageState state;

    private boolean selfTyper;

    private int scope;

    public Message(JSONObject payload) {
        super();
        this.domain = AuthService.getDomain();
        this.payload = payload;
        this.localTS = super.entityCreation;
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
        this.from = json.getLong("from");
        this.to = json.getLong("to");
        this.source = json.getLong("source");
        this.owner = json.getLong("owner");
        this.localTS = json.getLong("lts");
        this.remoteTS = json.getLong("rts");
        this.state = MessageState.parse(json.getInt("state"));
        this.scope = json.getInt("scope");

        if (json.has("payload")) {
            this.payload = json.getJSONObject("payload");
        }
    }

    public long getFrom() {
        return this.from;
    }

    public Contact getSender() {
        return this.sender;
    }

    public long getTo() {
        return this.to;
    }

    public Contact getReceiver() {
        return this.receiver;
    }

    public long getSource() {
        return this.source;
    }

    public Group getSourceGroup() {
        return this.sourceGroup;
    }

    public long getRemoteTimestamp() {
        return this.remoteTS;
    }

    public long getLocalTimestamp() {
        return this.localTS;
    }

    public MessageState getState() {
        return this.state;
    }

    public int getScope() {
        return this.scope;
    }

    public String getSummary() {
        return this.summary;
    }

    public Contact getPartner() {
        return this.selfTyper ? this.receiver : this.sender;
    }

    public boolean isFromGroup() {
        return this.source > 0;
    }

    public boolean isSelfTyper() {
        return this.selfTyper;
    }

    public void setSelfTyper(boolean value) {
        this.selfTyper = value;
    }

    public void setSender(Contact sender) {
        this.sender = sender;
    }

    public void setReceiver(Contact receiver) {
        this.receiver = receiver;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("domain", this.domain);
            json.put("from", this.from);
            json.put("to", this.to);
            json.put("source", this.source);
            json.put("owner", this.owner);
            json.put("lts", this.localTS);
            json.put("rts", this.remoteTS);
            json.put("state", this.state.code);
            json.put("scope", this.scope);
            json.put("payload", this.payload);
        } catch (JSONException e) {
            // Nothing
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
