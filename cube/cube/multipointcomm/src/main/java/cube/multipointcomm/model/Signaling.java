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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cube.contact.model.Contact;
import cube.contact.model.Device;
import cube.multipointcomm.util.MediaConstraint;
import cube.util.JSONable;

/**
 * 信令。
 */
public class Signaling implements JSONable {

    public long sn;

    public final String name;

    public final CommField field;

    public final Contact contact;

    public final Device device;

    public MediaConstraint mediaConstraint;

    public CommFieldEndpoint target;

    public SessionDescription sessionDescription;

    public IceCandidate candidate;

    public List<IceCandidate> candidates;

    public Contact invitee;

    public List<Long> invitees;

    public Contact caller;

    public Contact callee;

    public Signaling(String name, CommField commField, Contact contact, Device device) {
        this(0, name, commField, contact, device);
    }

    public Signaling(long sn, String name, CommField commField, Contact contact, Device device) {
        this.sn = sn;
        this.name = name;
        this.field = commField;
        this.contact = contact;
        this.device = device;
    }

    public Signaling(JSONObject json) throws JSONException {
        this.sn = json.getLong("sn");
        this.name = json.getString("name");
        this.field = new CommField(json.getJSONObject("field"));
        this.contact = new Contact(json.getJSONObject("contact"));
        this.device = new Device(json.getJSONObject("device"));

        if (json.has("description")) {
            JSONObject data = json.getJSONObject("description");
            this.sessionDescription = new SessionDescription(this.parseSessionDescriptionType(data.getString("type")),
                    data.getString("sdp"));
        }

        if (json.has("target")) {
            this.target = new CommFieldEndpoint(json.getJSONObject("target"));
        }

        if (json.has("candidate")) {
            JSONObject data = json.getJSONObject("candidate");
            this.candidate = new IceCandidate(data.getString("sdpMid"),
                    data.getInt("sdpMLineIndex"), data.getString("candidate"));
        }

        if (json.has("candidates")) {
            this.candidates = new ArrayList<>();

            JSONArray array = json.getJSONArray("candidates");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject data = array.getJSONObject(i);
                IceCandidate iceCandidate = new IceCandidate(data.getString("sdpMid"),
                        data.getInt("sdpMLineIndex"), data.getString("candidate"));
                this.candidates.add(iceCandidate);
            }
        }

        if (json.has("constraint")) {
            this.mediaConstraint = new MediaConstraint(json.getJSONObject("constraint"));
        }

        if (json.has("invitees")) {
            this.invitees = new ArrayList<>();

            JSONArray array = json.getJSONArray("invitees");
            for (int i = 0; i < array.length(); ++i) {
                this.invitees.add(array.getLong(i));
            }
        }

        if (json.has("invitee")) {
            this.invitee = new Contact(json.getJSONObject("invitee"));
        }

        if (json.has("caller")) {
            this.caller = new Contact(json.getJSONObject("caller"));
        }

        if (json.has("callee")) {
            this.callee = new Contact(json.getJSONObject("callee"));
        }
    }

    private SessionDescription.Type parseSessionDescriptionType(String type) {
        for (SessionDescription.Type sdt : SessionDescription.Type.values()) {
            if (sdt.name().equalsIgnoreCase(type)) {
                return sdt;
            }
        }

        return null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("sn", this.sn);
            json.put("name", this.name);
            json.put("field", this.field.toCompactJSON());
            json.put("contact", this.contact.toCompactJSON());
            json.put("device", this.device.toCompactJSON());

            if (null != this.sessionDescription) {
                JSONObject description = new JSONObject();
                description.put("type", this.sessionDescription.type.name().toLowerCase(Locale.ROOT));
                description.put("sdp", this.sessionDescription.description);
                json.put("description", description);
            }

            if (null != this.target) {
                json.put("target", this.target.toJSON());
            }

            if (null != this.candidate) {
                JSONObject candidate = new JSONObject();
                candidate.put("sdpMid", this.candidate.sdpMid);
                candidate.put("sdpMLineIndex", this.candidate.sdpMLineIndex);
                candidate.put("candidate", this.candidate.sdp);
                json.put("candidate", candidate);
            }

            if (null != this.mediaConstraint) {
                json.put("constraint", this.mediaConstraint.toJSON());
            }

            if (null != this.invitees) {
                JSONArray array = new JSONArray();
                for (Long id : this.invitees) {
                    array.put(id.longValue());
                }
                json.put("invitees", array);
            }

            if (null != this.caller) {
                json.put("caller", this.caller.toCompactJSON());
            }
            if (null != this.callee) {
                json.put("callee", this.callee.toCompactJSON());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
