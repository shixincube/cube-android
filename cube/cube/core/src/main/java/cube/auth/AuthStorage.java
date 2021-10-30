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

package cube.auth;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import cube.core.AbstractStorage;

/**
 * 存储器。
 */
public class AuthStorage extends AbstractStorage {

    private final static int VERSION = 1;

    public AuthStorage() {
        super();
    }

    /**
     * 开启存储器。
     * @param context 应用程序上下文。
     */
    public void open(Context context) {
        super.open(context, "CubeAuth.db", VERSION);
    }

    public AuthToken loadToken(String domain, String appKey) {
        AuthToken authToken = null;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM `token` WHERE `cid`=0 AND `domain`=? AND `app_key`=? ORDER BY `sn` DESC",
                new String[] { domain, appKey });
        if (cursor.moveToFirst()) {
            String data = cursor.getString(cursor.getColumnIndex("data"));
            try {
                authToken = new AuthToken(new JSONObject(data));
            } catch (JSONException e) {
                // Nothing
            }
        }
        cursor.close();

        this.closeReadableDatabase(db);

        return authToken;
    }

    public AuthToken loadToken(Long contactId, String domain, String appKey) {
        AuthToken authToken = null;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM `token` WHERE `cid`=? AND `domain`=? AND `app_key`=? ORDER BY `sn` DESC",
                new String[] { contactId.toString(), domain, appKey });
        if (cursor.moveToFirst()) {
            String data = cursor.getString(cursor.getColumnIndex("data"));
            try {
                authToken = new AuthToken(new JSONObject(data));
            } catch (JSONException e) {
                // Nothing
            }
        }
        cursor.close();

        this.closeReadableDatabase(db);
        return authToken;
    }

    public void saveToken(AuthToken token) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("domain", token.domain);
        values.put("app_key", token.appKey);
        values.put("cid", token.cid);
        values.put("code", token.code);
        values.put("data", token.toJSON().toString());

        db.insert("token", null, values);

        this.closeWritableDatabase(db);
    }

    public void updateToken(AuthToken token) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("cid", token.cid);
        values.put("data", token.toJSON().toString());
        db.update("token", values, "code=?", new String[] { token.code });

        this.closeWritableDatabase(db);
    }

    @Override
    protected void onDatabaseCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `token` (`sn` INTEGER PRIMARY KEY AUTOINCREMENT, `domain` TEXT, `app_key` TEXT, `cid` BIGINT DEFAULT 0, `code` TEXT, `data` TEXT)");
    }

    @Override
    protected void onDatabaseUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
    }
}
