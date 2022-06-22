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

import java.nio.charset.StandardCharsets;

import cube.auth.AuthService;
import cube.contact.model.Contact;
import cube.contact.model.Group;
import cube.core.handler.FailureHandler;
import cube.core.model.Entity;
import cube.messaging.MessagingService;
import cube.messaging.handler.LoadAttachmentHandler;

/**
 * 消息实体。
 */
public class Message extends Entity {

    private MessagingService service;

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
     * 消息的文件附件。
     */
    protected FileAttachment attachment;

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
    protected int scope;

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
     * @param service 指定消息服务。
     * @param json 消息的 JSON 结构数据。
     * @throws JSONException
     */
    public Message(MessagingService service, JSONObject json) throws JSONException {
        super(json);
        this.service = service;
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

        if (json.has("attachment")) {
            this.attachment = new FileAttachment(json.getJSONObject("attachment"));
        }

        if (json.has("summary")) {
            this.summary = json.getString("summary");
        }
    }

    /**
     * 构造函数。
     *
     * <b>Non-public API</b>
     *
     * @param id
     * @param timestamp
     * @param domain
     * @param owner
     * @param from
     * @param to
     * @param source
     * @param localTS
     * @param remoteTS
     * @param payload
     * @param state
     * @param scope
     */
    public Message(Long id, long timestamp, String domain, long owner, long from, long to, long source,
                   long localTS, long remoteTS, JSONObject payload, MessageState state, int scope,
                   FileAttachment attachment) {
        super(id, timestamp);
        this.domain = domain;
        this.owner = owner;
        this.from = from;
        this.to = to;
        this.source = source;
        this.localTS = localTS;
        this.remoteTS = remoteTS;
        this.payload = payload;
        this.state = state;
        this.scope = scope;
        this.attachment = attachment;
    }

    /**
     * 构造函数。
     *
     * <b>Non-public API</b>
     *
     * @param message
     */
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
        this.attachment = message.attachment;
        this.state = message.state;
        this.scope = message.scope;
        this.owner = message.owner;
        this.selfTyper = message.selfTyper;
        this.summary = message.summary;
        this.service = message.service;
    }

    /**
     * <b>Non-public API</b>
     *
     * @return
     */
    public long getOwner() {
        return this.owner;
    }

    /**
     * 获取消息发送人 ID 。
     *
     * @return 返回消息发送人 ID 。
     */
    public long getFrom() {
        return this.from;
    }

    /**
     * 获取消息发件人。
     *
     * @return 返回消息发件人。
     */
    public Contact getSender() {
        return this.sender;
    }

    /**
     * 获取消息接收人 ID 。
     *
     * @return 返回消息接收人 ID 。
     */
    public long getTo() {
        return this.to;
    }

    /**
     * 获取消息收件人。
     *
     * @return 返回消息收件人。
     */
    public Contact getReceiver() {
        return this.receiver;
    }

    /**
     * 获取消息来源。
     *
     * @return 返回消息来源。
     */
    public long getSource() {
        return this.source;
    }

    /**
     * 获取消息来源的群组。
     *
     * @return 返回消息来源的群组。
     */
    public Group getSourceGroup() {
        this.sourceGroup.entityLifeExpiry += 10000;
        return this.sourceGroup;
    }

    /**
     * 获取消息在服务器上的时间戳。
     *
     * @return 返回消息在服务器上的时间戳。
     */
    public long getRemoteTimestamp() {
        return this.remoteTS;
    }

    /**
     * 获取消息在本地的时间戳。
     *
     * @return 消息在本地的时间戳。
     */
    public long getLocalTimestamp() {
        return this.localTS;
    }

    /**
     * 获取消息状态。
     *
     * @return 返回消息状态。
     * @see MessageState
     */
    public MessageState getState() {
        return this.state;
    }

    /**
     * 返回消息作用域。
     *
     * @return 返回消息作用域。
     * @see MessageScope
     */
    public int getScope() {
        return this.scope;
    }

    /**
     * 获取消息默认摘要。
     *
     * @return 返回消息默认摘要。
     */
    public String getSummary() {
        return this.summary;
    }

    /**
     * 获取当前消息会话的对端联系人。
     *
     * @return 返回当前消息会话的对端联系人。
     */
    public Contact getPartner() {
        return this.selfTyper ? this.receiver : this.sender;
    }

    public Long getPartnerId() {
        return this.selfTyper ? this.to : this.from;
    }

    /**
     * 获取消息负载。
     *
     * @return 返回消息负载。
     */
    public JSONObject getPayload() {
        return this.payload;
    }

    /**
     * 获取消息的附件。
     *
     * @return 返回消息的附件。
     */
    public FileAttachment getAttachment() {
        return this.attachment;
    }

    /**
     * 获取消息类型。
     *
     * @return 返回消息类型。
     */
    public MessageType getType() {
        return this.type;
    }

    /**
     * 判断消息是否来自群组。
     *
     * @return 如果消息来自群组返回 {@code true} 。
     */
    public boolean isFromGroup() {
        return this.source > 0;
    }

    /**
     * 判断消息是否是当前账号撰写的。
     *
     * @return 返回消息是否是当前账号撰写的。
     */
    public boolean isSelfTyper() {
        return this.selfTyper;
    }

    /**
     * 判断当前账号是否是该消息发送人。
     *
     * @return 当前账号是否是该消息发送人。
     */
    public boolean isSender() {
        return this.selfTyper;
    }

    /**
     * <b>Non-public API</b>
     * @param attachment
     */
    public void setAttachment(FileAttachment attachment) {
        this.attachment = attachment;
    }

    /**
     * <b>Non-public API</b>
     * @param value
     */
    public void setSelfTyper(boolean value) {
        this.selfTyper = value;
    }

    /**
     * <b>Non-public API</b>
     * @param sender
     */
    public void setSender(Contact sender) {
        this.sender = sender;
    }

    /**
     * <b>Non-public API</b>
     * @param receiver
     */
    public void setReceiver(Contact receiver) {
        this.receiver = receiver;
    }

    /**
     * <b>Non-public API</b>
     * @param group
     */
    public void setSourceGroup(Group group) {
        this.sourceGroup = group;
    }

    /**
     * <b>Non-public API</b>
     * @param state
     */
    public void setState(MessageState state) {
        this.state = state;
    }

    /**
     * <b>Non-public API</b>
     * @param timestamp
     */
    public void setRemoteTS(long timestamp) {
        this.remoteTS = timestamp;
    }

    /**
     * <b>Non-public API</b>
     * @param service
     */
    public void setService(MessagingService service) {
        this.service = service;
    }

    /**
     * <b>Non-public API</b>
     * @param loadHandler
     * @param failureHandler
     */
    public void loadAttachment(LoadAttachmentHandler loadHandler,
                                   FailureHandler failureHandler) {
        this.service.loadMessageAttachment(this, loadHandler, failureHandler);
    }

    /**
     * <b>Non-public API</b>
     * @param payload
     */
    public void resetPayload(JSONObject payload) {
        this.payload = payload;
    }

    /**
     * 是否是空消息。
     *
     * <b>Non-public API</b>
     *
     * @return
     */
    public boolean isEmpty() {
        return false;
    }

    /**
     * <b>Non-public API</b>
     * @param owner
     * @param to
     * @param source
     */
    public void assign(long owner, long to, long source) {
        this.owner = owner;
        this.from = owner;
        this.to = to;
        this.source = source;
        this.localTS = this.timestamp;
        this.remoteTS = this.localTS;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (null != object && object instanceof Message) {
            Message other = (Message) object;
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
    public int getMemorySize() {
        int size = super.getMemorySize();
        size += 8 * 17;

        size += this.domain.getBytes(StandardCharsets.UTF_8).length;

        size += this.payload.toString().getBytes(StandardCharsets.UTF_8).length;

        if (null != this.attachment) {
            size += this.attachment.getMemorySize();
        }

        size += this.summary.getBytes(StandardCharsets.UTF_8).length;

        return size;
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

            if (null != this.attachment) {
                json.put("attachment", this.attachment.toJSON());
            }

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
        JSONObject json = this.toJSON();
        json.remove("summary");
        return json;
    }
}
