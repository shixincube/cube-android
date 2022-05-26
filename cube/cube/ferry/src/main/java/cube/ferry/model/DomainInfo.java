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

import org.json.JSONException;
import org.json.JSONObject;

import cube.core.model.Entity;

/**
 * 域信息。
 */
public class DomainInfo extends Entity {

    private String domainName;

    private long beginning;

    private long duration;

    private int limit;

    private String address;

    private String invitationCode;

    public DomainInfo(JSONObject json) throws JSONException {
        super(json);
        this.domainName = json.getString("domain");
        this.beginning = json.getLong("beginning");
        this.duration = json.getLong("duration");
        this.limit = json.getInt("limit");

        if (json.has("address")) {
            this.address = json.getString("address");
        }

        if (json.has("invitationCode")) {
            this.invitationCode = json.getString("invitationCode");
        }
    }

    public String getDomainName() {
        return this.domainName;
    }

    public long getBeginning() {
        return this.beginning;
    }

    public long getDuration() {
        return this.duration;
    }

    public int getLimit() {
        return this.limit;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return this.address;
    }

    public void setInvitationCode(String invitationCode) {
        this.invitationCode = invitationCode;
    }

    public String getInvitationCode() {
        return this.invitationCode;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("domain", this.domainName);
            json.put("beginning", this.beginning);
            json.put("duration", this.duration);
            json.put("limit", this.limit);

            if (null != this.address) {
                json.put("address", this.address);
            }

            if (null != this.invitationCode) {
                json.put("invitationCode", this.invitationCode);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
