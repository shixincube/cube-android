package cube.message.service.model;

import org.json.JSONException;
import org.json.JSONObject;

import cell.util.Utils;
import cube.common.JSONable;
import cube.contact.service.Self;
import cube.message.service.MessageState;
import cube.message.service.MessageType;

/**
 * 消息实体
 *
 * @author LiuFeng
 * @data 2020/9/2 16:13
 */
public class Message implements JSONable {
    private long id;
    private String sessionId;
    private String from;
    private String to;
    private String domain;
    private long source;
    private long localTimestamp;
    private long remoteTimestamp;
    private boolean recalled;
    private boolean receipted;
    private boolean traceless;
    private MessageType type;
    private MessageState state;
    protected JSONObject payload;

    public Message() {}

    public Message(long id, MessageType type) {
        this(id, type, null, null);
    }

    public Message(long id, MessageType type, String from, String to) {
        this.payload = new JSONObject();
        this.id = id;
        this.type = type;
        this.from = from;
        this.to = to;
        this.domain = Self.DOMAIN;

        if (id <= 0) {
            this.id = Utils.generateSerialNumber();
        }

        this.localTimestamp = System.currentTimeMillis();

        try {
            this.payload.put("type", type.type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Long getSource() {
        return source;
    }

    public void setSource(Long source) {
        this.source = source;
    }

    public long getLocalTimestamp() {
        return localTimestamp;
    }

    public void setLocalTimestamp(long localTimestamp) {
        this.localTimestamp = localTimestamp;
    }

    public long getRemoteTimestamp() {
        return remoteTimestamp;
    }

    public void setRemoteTimestamp(long remoteTimestamp) {
        this.remoteTimestamp = remoteTimestamp;
    }

    public boolean isRecalled() {
        return recalled;
    }

    public void setRecalled(boolean recalled) {
        this.recalled = recalled;
    }

    public boolean isReceipted() {
        return receipted;
    }

    public void setReceipted(boolean receipted) {
        this.receipted = receipted;
    }

    public boolean isTraceless() {
        return traceless;
    }

    public void setTraceless(boolean traceless) {
        this.traceless = traceless;
    }

    public MessageState getState() {
        return state;
    }

    public void setState(MessageState state) {
        this.state = state;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    /**
     * 拷贝
     *
     * @param src
     */
    public void clone(Message src) {
        this.id = src.id;
        this.from = src.from;
        this.to = src.to;
        this.domain = src.domain;
        this.source = src.source;
        this.payload = src.payload;
        this.sessionId = src.sessionId;
        this.recalled = src.recalled;
        this.receipted = src.receipted;
        this.traceless = src.traceless;
        this.state = src.state;
        this.type = src.type;
        this.localTimestamp = src.localTimestamp;
        this.remoteTimestamp = src.remoteTimestamp;
    }

    /**
     * 转JSON对象
     *
     * @return
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id);
            json.put("from", this.from);
            json.put("to", this.to);
            json.put("domain", this.domain);
            json.put("source", this.source);
            json.put("lts", this.localTimestamp);
            json.put("rts", this.remoteTimestamp);
            json.put("payload", this.payload);
            json.put("sessionId", this.sessionId);
            json.put("recalled", this.recalled);
            json.put("receipted", this.receipted);
            json.put("traceless", this.traceless);
            json.put("state", this.state.state);
            json.put("type", this.type.type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * json转实体对象
     *
     * @param json
     */
    public static Message create(JSONObject json) {
        JSONObject payload = json.optJSONObject("payload");
        if (payload != null && payload.has("type")) {
            MessageType type = MessageType.parse(payload.optString("type"));
            if (type == MessageType.Text) {
                TextMessage message = new TextMessage();
                message.setContent(payload.optString("content"));
                parseCommon(json, message);
                return message;
            }
        }

        return parseCommon(json, new Message());
    }

    private static Message parseCommon(JSONObject json, Message message) {
        JSONObject payload = json.optJSONObject("payload");
        message.id = json.optLong("id");
        message.type = MessageType.parse(payload != null ? payload.optString("type") : MessageType.UnKnown.type);
        message.from = json.optString("from");
        message.to = json.optString("to");
        message.payload = payload;
        message.source = json.optLong("source");
        message.domain = json.optString("domain");
        message.localTimestamp = json.optLong("lts");
        message.remoteTimestamp = json.optLong("rts");
        message.sessionId = json.optString("sessionId");
        message.recalled = json.optBoolean("recalled");
        message.receipted = json.optBoolean("receipted");
        message.traceless = json.optBoolean("traceless");
        message.state = MessageState.parse(json.optInt("state"));
        return message;
    }
}
