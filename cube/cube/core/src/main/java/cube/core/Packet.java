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

package cube.core;

import org.json.JSONException;
import org.json.JSONObject;

import cell.util.Utils;

/**
 * 数据包描述。
 */
public class Packet {

    /**
     * 数据包序号。
     */
    public final Long sn;

    /**
     * 数据包包名。
     */
    public final String name;

    /**
     * 数据包负载数据。
     */
    private JSONObject data;

    /**
     * 数据包应答状态。
     */
    public PipelineState state;

    public Packet(String name) {
        this(Utils.generateUnsignedSerialNumber(), name, null);
    }

    public Packet(String name, JSONObject data) {
        this(Utils.generateUnsignedSerialNumber(), name, data);
    }

    public Packet(Long sn, String name, JSONObject data) {
        this.sn = sn;
        this.name = name;
        this.data = data;
    }

    public Packet(JSONObject packetJSON) throws JSONException {
        this.sn = packetJSON.getLong("sn");
        this.name = packetJSON.getString("name");
        this.data = packetJSON.getJSONObject("data");
    }

    public void setData(JSONObject data) {
        this.data = data;
    }

    public JSONObject getData() {
        return this.data;
    }

    /**
     * 提取服务的状态码。
     * @return
     */
    public int extractServiceStateCode() {
        try {
            return this.data.getInt("code");
        } catch (JSONException e) {
            // Nothing
        }
        return -1;
    }

    /**
     * 提取服务模块的数据。
     * @return
     */
    public JSONObject extractServiceData() {
        try {
            return this.data.getJSONObject("data");
        } catch (JSONException e) {
            // Nothing
        }
        return null;
    }
}
