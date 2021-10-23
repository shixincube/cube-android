/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Shixin Cube Team.
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

package com.shixincube.app.model;

import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

import cube.util.JSONable;

/**
 * 账号。
 */
public class Account implements JSONable {

    public long id;

    public String account;

    @SerializedName("phone")
    public String phoneNumber;

    public String name;

    public String avatar;

    public int state;

    public String region;

    public String department;

    public Account() {
    }

    public Account(JSONObject json) throws JSONException {
        this.id = json.getLong("id");
        this.account = json.getString("account");
        this.phoneNumber = json.getString("phone");
        this.name = json.getString("name");
        this.avatar = json.getString("avatar");
        this.state = json.getInt("state");
        this.region = json.getString("region");
        this.department = json.getString("department");
    }

    public static String getAvatar(JSONObject json) {
        try {
            return json.getString("avatar");
        } catch (JSONException e) {
            return "default";
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id);
            json.put("account", this.account);
            json.put("phone", this.phoneNumber);
            json.put("name", this.name);
            json.put("avatar", this.avatar);
            json.put("state", this.state);
            json.put("region", this.region);
            json.put("department", this.department);
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
