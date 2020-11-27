package cube.contact.service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cell.core.talk.TalkContext;
import cube.common.JSONable;

/**
 * 联系人实体。
 */
public class Contact implements JSONable {

    /**
     * 联系人 ID 。
     */
    private Long id;

    /**
     * 联系人显示名。
     */
    private String name;

    /**
     * 指定联系人所在的域。
     */
    private String domain;

    /**
     * 联系携带的上下文 JSON 数据。
     */
    private JSONObject context;

    /**
     * 联系人的设备列表。
     */
    private List<Device> deviceList;

    public Contact(Long id, String name, String domain) {
        this.id = id;
        this.name = name;
        this.domain = domain;
        this.deviceList = new ArrayList<>(2);
    }

    public Contact(JSONObject json) {
        this.deviceList = new ArrayList<>(2);

        try {
            this.id = json.getLong("id");
            this.name = json.getString("name");
            this.domain = json.getString("domain");

            if (json.has("devices")) {
                JSONArray array = json.getJSONArray("devices");
                for (int i = 0; i < array.length(); ++i) {
                    JSONObject devJson = array.getJSONObject(i);
                    Device device = new Device(devJson);
                    this.addDevice(device);
                }
            }

            if (json.has("context")) {
                this.context = json.getJSONObject("context");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Contact(JSONObject json, TalkContext talkContext) {
        this.deviceList = new ArrayList<>(2);

        try {
            this.id = json.getLong("id");
            this.name = json.getString("name");
            this.domain = json.getString("domain");

            if (json.has("devices")) {
                JSONArray array = json.getJSONArray("devices");
                for (int i = 0; i < array.length(); ++i) {
                    JSONObject devJson = array.getJSONObject(i);
                    Device device = new Device(devJson);
                    this.addDevice(device);
                }
            }

            if (json.has("context")) {
                this.context = json.getJSONObject("context");
            }

            // 绑定设备到上下文
            if (json.has("device")) {
                JSONObject deviceJson = json.getJSONObject("device");
                Device device = new Device(deviceJson, talkContext);
                this.addDevice(device);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取联系人的 ID 。
     *
     * @return 返回联系人的 ID 。
     */
    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public JSONObject getContext() {
        return this.context;
    }

    public void setContext(JSONObject context) {
        this.context = context;
    }

    public Device addDevice(Device device) {
        Device invalid = null;
        int index = this.deviceList.indexOf(device);
        if (index >= 0) {
            invalid = this.deviceList.get(index);
            this.deviceList.remove(index);
        }

        this.deviceList.add(device);

        return invalid;
    }

    public Device addDevice(String name, String platform, TalkContext talkContext) {
        Device invalid = null;
        Device device = new Device(name, platform, talkContext);

        int index = this.deviceList.indexOf(device);
        if (index >= 0) {
            invalid = this.deviceList.get(index);
            this.deviceList.remove(index);
        }

        this.deviceList.add(device);

        return invalid;
    }

    public void removeDevice(Device device) {
        this.deviceList.remove(device);
    }

    /**
     * 通过客户端的 TalkContext 匹配获取对应的设备。
     * 该方法主要提供给网关节点使用。
     *
     * @param talkContext
     * @return
     */
    public Device getDevice(TalkContext talkContext) {
        Device device = null;
        for (int i = 0, size = this.deviceList.size(); i < size; ++i) {
            device = this.deviceList.get(i);
            if (device.getTalkContext() == talkContext) {
                return device;
            }
        }
        return null;
    }

    /**
     * 获取设备列表。
     *
     * @return 返回设备列表。
     */
    public List<Device> getDeviceList() {
        return new ArrayList<Device>(this.deviceList);
    }

    /**
     * 是否包含指定设备。
     *
     * @param device
     * @return
     */
    public boolean hasDevice(Device device) {
        return this.deviceList.contains(device);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Contact) {
            Contact other = (Contact) object;
            return other.id.longValue() == this.id.longValue();
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id);
            json.put("name", this.name);
            json.put("domain", this.domain);

            JSONArray array = new JSONArray();
            for (int i = 0, len = this.deviceList.size(); i < len; ++i) {
                Device device = this.deviceList.get(i);
                array.put(device.toJSON());
            }
            json.put("devices", array);

            if (null != this.context) {
                json.put("context", this.context);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
