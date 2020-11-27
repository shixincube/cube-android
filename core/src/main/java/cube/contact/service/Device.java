package cube.contact.service;

import org.json.JSONException;
import org.json.JSONObject;

import cell.core.talk.TalkContext;
import cube.common.JSONable;

/**
 * 设备实体描述。
 */
public class Device implements JSONable {

    /**
     * 设备名称。
     */
    private String name;

    /**
     * 设备平台描述。
     */
    private String platform;

    /**
     * 设备当前接入的地址。
     */
    private String address;

    /**
     * 设备当前接入的端口。
     */
    private int port = 0;

    /**
     * 设备当前的会话上下文。
     */
    private TalkContext talkContext;

    /**
     * 设备信息的 Hash 值。
     */
    private int hash = 0;

    public Device(String name, String platform) {
        this.name = name;
        this.platform = platform;
    }

    public Device(String name, String platform, TalkContext talkContext) {
        this.name = name;
        this.platform = platform;
        this.address = talkContext.getSessionHost();
        this.port = talkContext.getSessionPort();
        this.talkContext = talkContext;
    }

    public Device(JSONObject deviceJson) {
        try {
            this.name = deviceJson.getString("name");
            this.platform = deviceJson.getString("platform");

            if (deviceJson.has("address")) {
                this.address = deviceJson.getString("address");
            }

            if (deviceJson.has("port")) {
                this.port = deviceJson.getInt("port");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Device(JSONObject deviceJson, TalkContext talkContext) {
        try {
            this.name = deviceJson.getString("name");
            this.platform = deviceJson.getString("platform");

            if (deviceJson.has("address")) {
                this.address = deviceJson.getString("address");
            }
            else {
                this.address = talkContext.getSessionHost();
            }

            if (deviceJson.has("port")) {
                this.port = deviceJson.getInt("port");
            }
            else {
                this.port = talkContext.getSessionPort();
            }

            this.talkContext = talkContext;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return this.name;
    }

    public String getPlatform() {
        return this.platform;
    }

    public String getAddress() {
        return this.address;
    }

    public int getPort() {
        return this.port;
    }

    public TalkContext getTalkContext() {
        return this.talkContext;
    }

    public boolean isOnline() {
        return (null != this.talkContext) /*&& this.talkContext.isValid()*/;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("name", this.name);
            json.put("platform", this.platform);
            if (null != this.address) {
                json.put("address", this.address);
            }
            if (0 != this.port) {
                json.put("port", this.port);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Device) {
            Device other = (Device) obj;
            if (this.name.equals(other.name) && this.platform.equals(other.platform)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        if (0 == this.hash) {
            this.hash = this.name.hashCode() * 3 + this.platform.hashCode() * 7;
        }

        return this.hash;
    }
}
