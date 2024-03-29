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

package com.shixincube.app.model;

import com.google.gson.annotations.SerializedName;
import com.shixincube.app.util.PinyinUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import cube.contact.model.Contact;
import cube.util.JSONable;

/**
 * 账号。
 */
public class Account implements JSONable {

    /**
     * 默认的头像图片名称。
     */
    public final static String DefaultAvatarName = "default";

    public long id;

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
        this.phoneNumber = json.getString("phone");
        this.name = json.getString("name");
        this.avatar = json.getString("avatar");
        this.state = json.getInt("state");
        this.region = json.getString("region");
        this.department = json.getString("department");
    }

    public static String getAvatar(Contact contact) {
        return getAvatar(contact.getContext());
    }

    private static String getAvatar(JSONObject json) {
        if (null == json) {
            return "default";
        }

        try {
            return json.getString("avatar");
        } catch (JSONException e) {
            return "default";
        }
    }

    /**
     * 设置用户名拼写。
     *
     * @param contact
     */
    public static String setNameSpelling(Contact contact) {
        if (null == contact) {
            return "";
        }

        String pinyin = contact.getPriorityName();
        try {
            if (null != contact.getContext()) {
                JSONObject context = contact.getContext();
                if (context.has("nameSpelling")) {
                    return context.getString("nameSpelling");
                }

                pinyin = PinyinUtils.getPinyin(contact.getPriorityName());
                context.put("nameSpelling", pinyin);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return pinyin;
    }

    /**
     * 获取用户的拼写形式。
     *
     * @param contact
     * @return
     */
    public static String getNameSpelling(Contact contact) {
        String pinyin = contact.getPriorityName();
        try {
            if (null != contact.getContext()) {
                JSONObject context = contact.getContext();
                if (context.has("nameSpelling")) {
                    pinyin = context.getString("nameSpelling");
                }
                else {
                    pinyin = PinyinUtils.getPinyin(contact.getPriorityName());
                }
            }
            else {
                pinyin = PinyinUtils.getPinyin(contact.getPriorityName());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return pinyin.toUpperCase(Locale.ROOT);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id);
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
