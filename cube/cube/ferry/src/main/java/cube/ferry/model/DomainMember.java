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

package cube.ferry.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import cube.core.model.Entity;

/**
 * 域成员。
 */
public class DomainMember extends Entity implements Parcelable {

    private String domainName;

    private Long contactId;

    private JoinWay joinWay;

    private long joinTime;

    private Role role;

    private int state;

    public DomainMember(String domainName, Long contactId, JoinWay joinWay, long joinTime, Role role) {
        super(contactId);
        this.domainName = domainName;
        this.contactId = contactId;
        this.joinWay = joinWay;
        this.joinTime = joinTime;
        this.role = role;
        this.state = 0;
    }

    public DomainMember(JSONObject json) throws JSONException {
        super(json);
        this.domainName = json.getString("domain");
        this.contactId = json.getLong("contactId");
        this.joinWay = JoinWay.parse(json.getInt("joinWay"));
        this.joinTime = json.getLong("joinTime");
        this.role = Role.parse(json.getInt("role"));
        this.state = json.getInt("state");
    }

    protected DomainMember(Parcel in) {
        super(in.readLong(), in.readLong());
        this.domainName = in.readString();
        this.contactId = in.readLong();
        this.joinWay = JoinWay.parse(in.readInt());
        this.joinTime = in.readLong();
        this.role = Role.parse(in.readInt());
        this.state = in.readInt();
    }

    public static final Creator<DomainMember> CREATOR = new Creator<DomainMember>() {
        @Override
        public DomainMember createFromParcel(Parcel in) {
            return new DomainMember(in);
        }

        @Override
        public DomainMember[] newArray(int size) {
            return new DomainMember[size];
        }
    };

    public String getDomainName() {
        return this.domainName;
    }

    public Long getContactId() {
        return this.contactId;
    }

    public void setJoinWay(JoinWay joinWay) {
        this.joinWay = joinWay;
    }

    public JoinWay getJoinWay() {
        return this.joinWay;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Role getRole() {
        return this.role;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getState() {
        return this.state;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("domain", this.domainName);
            json.put("contactId", this.contactId.longValue());
            json.put("joinWay", this.joinWay.code);
            json.put("joinTime", this.joinTime);
            json.put("role", this.role.code);
            json.put("state", this.state);
        } catch (JSONException e) {
            // Nothing
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeLong(this.timestamp);
        dest.writeString(this.domainName);
        dest.writeLong(this.contactId);
        dest.writeInt(this.joinWay.code);
        dest.writeLong(this.joinTime);
        dest.writeInt(this.role.code);
        dest.writeInt(this.state);
    }
}
