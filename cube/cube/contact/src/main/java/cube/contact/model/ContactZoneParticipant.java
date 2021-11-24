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

import org.json.JSONException;
import org.json.JSONObject;

import cube.core.model.Entity;

/**
 * 联系人分区参与者。
 */
public class ContactZoneParticipant extends Entity {

    private ContactZoneParticipantType type;

    private ContactZoneParticipantState state;

    private String postscript;

    private Contact contact;

    private Group group;

    public ContactZoneParticipant(Long id, long timestamp, ContactZoneParticipantType type, ContactZoneParticipantState state, String postscript) {
        super(id, timestamp);
        this.type = type;
        this.state = state;
        this.postscript = postscript;
    }

    public ContactZoneParticipant(JSONObject json) throws JSONException {
        super(json);
        this.type = ContactZoneParticipantType.parse(json.getInt("type"));
        this.state = ContactZoneParticipantState.parse(json.getInt("state"));
        this.postscript = json.getString("postscript");
    }

    public Contact getContact() {
        return this.contact;
    }

    public Group getGroup() {
        return this.group;
    }

    public ContactZoneParticipantType getType() {
        return this.type;
    }

    public ContactZoneParticipantState getState() {
        return this.state;
    }

    public String getPostscript() {
        return this.postscript;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toCompactJSON();
        try {
            json.put("type", this.type.code);
            json.put("state", this.state.code);
            json.put("postscript", this.postscript);
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
