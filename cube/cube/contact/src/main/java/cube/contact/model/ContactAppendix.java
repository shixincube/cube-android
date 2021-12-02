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

import java.nio.charset.StandardCharsets;

import cube.contact.ContactService;
import cube.core.model.Cacheable;
import cube.util.JSONable;

/**
 * 联系人附件。
 */
public class ContactAppendix implements JSONable, Cacheable {

    private ContactService service;

    private Contact contact;

    private String remarkName;

    public ContactAppendix(ContactService service, Contact contact, JSONObject json) throws JSONException {
        this.service = service;
        this.contact = contact;

        if (json.has("remarkName")) {
            this.remarkName = json.getString("remarkName");
        }
    }

    /**
     * 获取该附录的联系人。
     *
     * @return 返回该附录的联系人。
     */
    public Contact getContact() {
        return this.contact;
    }

    /**
     * 设置该联系人的备注名。
     *
     * @param remarkName 指定备注名。
     */
    public void setRemarkName(String remarkName) {
        this.remarkName = remarkName;
    }

    /**
     * 获取备注名。
     *
     * @return 返回备注名。
     */
    public String getRemarkName() {
        return this.remarkName;
    }

    /**
     * 是否设置了配置名。
     *
     * @return 如果设置了备注名返回 {@code true} 。
     */
    public boolean hasRemarkName() {
        return (null != this.remarkName) && (this.remarkName.length() > 0);
    }

    @Override
    public int getMemorySize() {
        int base = 8 + 8 + 8 + 8;
        if (this.hasRemarkName()) {
            base += this.remarkName.getBytes(StandardCharsets.UTF_8).length;
        }
        return base;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("contact", this.contact.toCompactJSON());

            if (this.hasRemarkName()) {
                json.put("remarkName", this.remarkName);
            }
        } catch (JSONException e) {
            // Nothing
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
