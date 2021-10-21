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

package cube.contact.model;

import org.json.JSONException;
import org.json.JSONObject;

import cube.contact.ContactService;
import cube.util.JSONable;

/**
 * 联系人附件。
 */
public class ContactAppendix implements JSONable {

    private ContactService service;

    private Contact owner;

    private String remarkName;

    public ContactAppendix(ContactService service, Contact owner, JSONObject json) throws JSONException {
        this.service = service;
        this.owner = owner;

        if (json.has("remarkName")) {
            this.remarkName = json.getString("remarkName");
        }
    }

    public Contact getOwner() {
        return this.owner;
    }

    /**
     * 设置该联系人的备注名。
     *
     * @param remarkName
     */
    public void setRemarkName(String remarkName) {
        this.remarkName = remarkName;
    }

    /**
     * 返回备注名。
     *
     * @return
     */
    public String getRemarkName() {
        return this.remarkName;
    }

    public boolean hasRemarkName() {
        return (null != this.remarkName) && (this.remarkName.length() > 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("owner", this.owner.toCompactJSON());

            if (this.hasRemarkName()) {
                json.put("remarkName", this.remarkName);
            }
        } catch (JSONException e) {
            // Nothing
        }
        return json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
