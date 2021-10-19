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

package com.shixincube.app.manager;

import android.content.Context;
import android.content.SharedPreferences;

import com.shixincube.app.AppConsts;
import com.shixincube.app.model.Account;

/**
 * 账号辅助操作函数库。
 */
public class AccountHelper {

    private final static String SP_NAME = "cube.account";

    private static AccountHelper instance;

    private Context context;

    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    private AccountHelper(Context context) {
        this.context = context;
        this.sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        this.editor = this.sp.edit();
    }

    public static AccountHelper getInstance(Context context) {
        if (null == AccountHelper.instance) {
            AccountHelper.instance = new AccountHelper(context);
        }
        return AccountHelper.instance;
    }

    public boolean checkValidToken() {
        String tokenCode = this.loadTokenCode();
        if (null == tokenCode) {
            return false;
        }

        long expire = this.loadTokenExpire();
        if (expire <= System.currentTimeMillis()) {
            return false;
        }

        return true;
    }

    public Account getCurrentAccount() {
        return null;
    }

    public void saveToken(String tokenCode, long expire) {
        this.editor.putString(AppConsts.TOKEN_CODE, tokenCode);
        this.editor.putLong(AppConsts.TOKEN_EXPIRE, expire);
    }

    private String loadTokenCode() {
        String tokenCode = this.sp.getString(AppConsts.TOKEN_CODE, "");
        return tokenCode.length() > 0 ? tokenCode : null;
    }

    private long loadTokenExpire() {
        return this.sp.getLong(AppConsts.TOKEN_EXPIRE, 0L);
    }
}
