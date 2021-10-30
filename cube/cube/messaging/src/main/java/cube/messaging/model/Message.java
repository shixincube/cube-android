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

    /**
     * 消息所在的域。
     */
    public final String domain;

    /**
     * 消息发送方 ID 。
     */
    private long from;

    /**
     * 消息发件人。
     */
    private Contact sender;

    /**
     * 消息接收方 ID 。
     */
    private long to;

    /**
     * 消息收件人。
     */
    private Contact receiver;

    /**
     * 消息的收发源。该属性表示消息在一个广播域里的域标识或者域 ID 。
     */
    private long source;

    /**
     * 消息的收发群组。
     */
    private Group sourceGroup;

    /**
     * 消息当前的持有人。
     */
    private long owner;

    /**
     * 本地时间戳。
     */
    private long localTS;

    /**
     * 服务器端时间戳。
     */
    private long remoteTS;

    /**
     * 消息负载数据。
     */
    protected JSONObject payload;

    /**
     * 消息的摘要内容。
     */
    protected String summary;

    /**
     * 消息状态描述。
     */
    private MessageState state;

    /**
     * 自己是否是该消息的撰写人。
     */
    private boolean selfTyper;

    /**
     * 消息的作用域。
     * @see MessageScope
     */
    private int scope;

    /**
     * 消息类型。
     */
    protected MessageType type = MessageType.Unknown;

    public Message() {
        this(new JSONObject());
    }

    /**
     * 构造函数。
     *
     * @param payload 指定消息负载。
     */
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

    /**
     * 构造函数。
     *
     * @param service
     * @param json 消息的 JSON 结构数据。
     * @throws JSONException
     */
    public Message(MessagingService service, JSONObject json) throws JSONException {
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

        if (json.has("summary")) {
            this.summary = json.getString("summary");
        }
    }

    public Message(Message message) {
        super(message.id, message.timestamp);
        this.domain = message.domain;
        this.from = message.from;
        this.sender = message.sender;
        this.to = message.to;
        this.receiver = message.receiver;
        this.source = message.source;
        this.sourceGroup = message.sourceGroup;
        this.localTS = message.localTS;
        this.remoteTS = message.remoteTS;
        this.payload = message.payload;
        this.state = message.state;
        this.scope = message.scope;
        this.owner = message.owner;
        this.selfTyper = message.selfTyper;
        this.summary = message.summary;
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

    public JSONObject getPayload() {
        return this.payload;
    }

    public MessageType getType() {
        return this.type;
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

            if (null != this.summary) {
                json.put("summary", this.summary);
            }
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
