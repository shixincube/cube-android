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

/**
 * 账号辅助操作函数库。
 */
public class AccountHelper {

    private final static String SP_NAME = "cube.account";

    private static AccountHelper instance;

    private Context context;

    private SharedPreferences sp;

    private AccountHelper(Context context) {
        this.context = context;

        this.sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    public static AccountHelper getInstance(Context context) {
        if (null == AccountHelper.instance) {
            AccountHelper.instance = new AccountHelper(context);
        }
        return AccountHelper.instance;
    }

    public boolean checkValidToken() {
        return false;
    }

    private String loadTokenCode() {
        String tokenCode = this.sp.getString(AppConsts.TOKEN_CODE, "");
        return tokenCode.length() > 0 ? tokenCode : null;
    }
}
