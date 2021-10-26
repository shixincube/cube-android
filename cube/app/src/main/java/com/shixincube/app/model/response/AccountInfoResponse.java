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

package com.shixincube.app.model.response;

import com.google.gson.annotations.SerializedName;
import com.shixincube.app.model.Account;

/**
 * 账号信息。
 */
public class AccountInfoResponse {

    public long id;

    public String account;

    public String name;

    @SerializedName("phone")
    public String phoneNumber;

    public String avatar;

    public int state;

    public String region;

    public String department;

    public AccountInfoResponse() {
    }

    public Account toAccount() {
        Account account = new Account();
        account.id = this.id;
        account.account = (null != this.account) ? this.account : "";
        account.name = this.name;
        account.phoneNumber = this.phoneNumber;
        account.avatar = this.avatar;
        account.state = this.state;
        account.region = this.region;
        account.department = this.department;
        return account;
    }
}
