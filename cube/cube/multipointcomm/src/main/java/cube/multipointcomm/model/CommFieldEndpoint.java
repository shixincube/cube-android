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

package cube.multipointcomm.model;

import org.json.JSONException;
import org.json.JSONObject;

import cube.auth.AuthService;
import cube.contact.model.Contact;
import cube.contact.model.Device;
import cube.core.model.Entity;
import cube.multipointcomm.RTCDevice;

/**
 * 通讯场域里的媒体节点。
 */
public class CommFieldEndpoint extends Entity {

    private String name;

    private Contact contact;

    private Device device;

    protected CommFieldEndpointState state;

    protected CommField field;

    protected RTCDevice rtcDevice;

    protected int volume = 50;

    protected boolean videoEnabled;
    protected boolean videoStreamEnabled;

    protected boolean audioEnabled;
    protected boolean audioStreamEnabled;

    public CommFieldEndpoint(Long id, Contact contact, Device device) {
        super(id);
        this.name = contact.id.toString() + "_" + device.name + "_" + device.platform;
        this.contact = contact;
        this.device = device;
        this.state = CommFieldEndpointState.Normal;
    }

    public Contact getContact() {
        return this.contact;
    }

    public Device getDevice() {
        return this.device;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("domain", AuthService.getDomain());
            json.put("contact", this.contact.toCompactJSON());
            json.put("device", this.device.toJSON());
            json.put("state", this.state.code);
            json.put("name", this.name);

            JSONObject video = new JSONObject();
            video.put("enabled", this.videoEnabled);
            video.put("streamEnabled", this.videoStreamEnabled);
            json.put("video", video);

            JSONObject audio = new JSONObject();
            audio.put("enabled", this.audioEnabled);
            audio.put("streamEnabled", this.audioStreamEnabled);
            json.put("audio", audio);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        try {
            json.put("domain", AuthService.getDomain());
            json.put("contact", this.contact.toCompactJSON());
            json.put("device", this.device.toCompactJSON());
            json.put("state", this.state.code);
            json.put("name", this.name);

            JSONObject video = new JSONObject();
            video.put("enabled", this.videoEnabled);
            video.put("streamEnabled", this.videoStreamEnabled);
            json.put("video", video);

            JSONObject audio = new JSONObject();
            audio.put("enabled", this.audioEnabled);
            audio.put("streamEnabled", this.audioStreamEnabled);
            json.put("audio", audio);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
