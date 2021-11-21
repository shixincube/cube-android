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

package cube.contact.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 联系人实体。
 */
public class Contact extends AbstractContact {

    /**
     * 当前有效的设备列表。
     */
    public final List<Device> devices;

    /**
     * 附录。
     */
    private ContactAppendix appendix;

    public Contact(Long id, String name) {
        super(id, name);
        this.devices = new ArrayList<>();
    }

    public Contact(Long id, String name, JSONObject context) {
        super(id, name);
        this.context = context;
        this.devices = new ArrayList<>();
    }

    public Contact(Long id, String name, String domain) {
        super(id, name, domain);
        this.devices = new ArrayList<>();
    }

    public Contact(Long id, String name, String domain, JSONObject context) {
        super(id, name, domain);
        this.context = context;
        this.devices = new ArrayList<>();
    }

    public Contact(Long id, String name, String domain, long timestamp) {
        super(id, name, domain, timestamp);
        this.devices = new ArrayList<>();
    }

    public Contact(JSONObject json) throws JSONException {
        super(json);
        this.devices = new ArrayList<>();

        if (json.has("devices")) {
            JSONArray array = json.getJSONArray("devices");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject data = array.getJSONObject(i);
                Device device = new Device(data);
                this.addDevice(device);
            }
        }
    }

    /**
     * 获取联系人优先显示的名称。
     *
     * @return 返回联系人优先显示的名称。
     */
    public String getPriorityName() {
        if (null == this.appendix) {
            return this.name;
        }

        if (this.appendix.hasRemarkName()) {
            return this.appendix.getRemarkName();
        }

        return this.name;
    }

    /**
     * 添加联系人使用的设备。
     *
     * @param device 设备描述。
     */
    public void addDevice(Device device) {
        if (!this.devices.contains(device)) {
            this.devices.add(device);
        }
    }

    /**
     * 移除联系人使用的设备。
     *
     * @param device 设备描述。
     */
    public void removeDevice(Device device) {
        this.devices.remove(device);
    }

    /**
     * 设置附录。
     *
     * @param appendix
     */
    public void setAppendix(ContactAppendix appendix) {
        this.appendix = appendix;
    }

    /**
     * 获取附录。
     *
     * @return
     */
    public ContactAppendix getAppendix() {
        return this.appendix;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (null == object || !(object instanceof Contact)) {
            return false;
        }

        Contact other = (Contact) object;
        return other.id.equals(this.id) && other.domain.equals(this.domain);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        JSONArray list = new JSONArray();
        for (Device device : this.devices) {
            list.put(device.toJSON());
        }
        try {
            json.put("devices", list);
        } catch (JSONException e) {
            // Nothing
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        if (json.has("context")) {
            json.remove("context");
        }
        return json;
    }
}
