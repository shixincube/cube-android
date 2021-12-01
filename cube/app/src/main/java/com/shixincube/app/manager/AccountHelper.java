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

package com.shixincube.app.manager;

import android.content.Context;
import android.content.SharedPreferences;

import com.shixincube.app.AppConsts;
import com.shixincube.app.api.Explorer;
import com.shixincube.app.model.Account;

import org.json.JSONException;
import org.json.JSONObject;

import cell.util.NetworkUtils;
import cube.contact.ContactDataProvider;
import cube.contact.model.Contact;
import cube.util.LogUtils;
import cube.util.MutableJSONObject;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 账号辅助操作函数库。
 */
public class AccountHelper implements ContactDataProvider {

    private final static String SP_NAME = "cube.account";

    private static AccountHelper instance;

    private Context context;

    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    private String tokenCode;

    private Account current;

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

    public static AccountHelper getInstance() {
        return AccountHelper.instance;
    }

    /**
     * 检查令牌是否有效。
     *
     * @return
     */
    public boolean checkValidToken() {
        String tokenCode = this.loadTokenCode();
        if (null == tokenCode) {
            return false;
        }

        long expire = this.loadTokenExpire();
        if (expire <= System.currentTimeMillis()) {
            return false;
        }

        // 令牌有效，检查账号数据
        Account account = getCurrentAccount();
        if (null == account) {
            return false;
        }

        this.tokenCode = tokenCode;

        return true;
    }

    /**
     * 获取令牌码。
     *
     * @return
     */
    public String getTokenCode() {
        return this.tokenCode;
    }

    public void setCurrentAccount(Account account) {
        this.current = account;
        this.editor.putString(AppConsts.APP_ACCOUNT, account.toJSON().toString());
        this.editor.commit();
    }

    public Account getCurrentAccount() {
        if (null == this.current) {
            String jsonString = this.sp.getString(AppConsts.APP_ACCOUNT, "");
            if (jsonString.length() > 3) {
                try {
                    this.current = new Account(new JSONObject(jsonString));
                } catch (JSONException e) {
                    e.printStackTrace();
                    this.current = null;
                }
            }
        }

        return this.current;
    }

    /**
     * 更新当前账号。
     *
     * @param name
     * @param avatar
     */
    public void updateCurrentAccount(String name, String avatar) {
        if (null == this.current) {
            return;
        }

        if (null != name) {
            this.current.name = name;
        }

        if (null != avatar) {
            this.current.avatar = avatar;
        }

        this.editor.putString(AppConsts.APP_ACCOUNT, this.current.toJSON().toString());
        this.editor.commit();
    }

    public void saveToken(String tokenCode, long expire) {
        // 令牌赋值
        this.tokenCode = tokenCode;

        this.editor.putString(AppConsts.TOKEN_CODE, tokenCode);
        this.editor.putLong(AppConsts.TOKEN_EXPIRE, expire);
        this.editor.commit();
    }

    /**
     * 使用账号创建联系人。
     *
     * @param account
     * @return
     */
    public Contact createContact(Account account) {
        return new Contact(account.id, account.name, account.toJSON());
    }

    @Override
    public JSONObject needContactContext(Contact contact) {
        LogUtils.d(this.getClass().getSimpleName(), "#needContactContext : " + contact.id);

        if (!NetworkUtils.isAvailable(this.context)) {
            return null;
        }

        MutableJSONObject data = new MutableJSONObject();

        Explorer.getInstance().getAccountInfo(contact.getId(), this.tokenCode)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(accountInfoResponse -> {
                // 转为 JSON
                data.jsonObject = accountInfoResponse.toAccount().toJSON();

                // 设置名称
                contact.setName(accountInfoResponse.name);

                synchronized (data) {
                    data.notify();
                }
            }, throwable -> {
                LogUtils.w("AccountHelper", throwable);

                synchronized (data) {
                    data.notify();
                }
            });

        synchronized (data) {
            try {
                data.wait(10 * 1000);
            } catch (InterruptedException e) {
                // Nothing
            }
        }

        return data.jsonObject;
    }

    private String loadTokenCode() {
        String tokenCode = this.sp.getString(AppConsts.TOKEN_CODE, "");
        return tokenCode.length() > 0 ? tokenCode : null;
    }

    private long loadTokenExpire() {
        return this.sp.getLong(AppConsts.TOKEN_EXPIRE, 0L);
    }
}
