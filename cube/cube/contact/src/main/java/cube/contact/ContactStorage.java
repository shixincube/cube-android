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

package cube.contact;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONException;
import org.json.JSONObject;

import cube.contact.model.Contact;
import cube.contact.model.ContactAppendix;
import cube.core.Storage;

/**
 * 联系人模块存储器。
 */
public class ContactStorage implements Storage {

    private final static int VERSION = 1;

    private ContactService service;

    private SQLite sqlite;

    private Long contactId;

    private String domain;

    public ContactStorage(ContactService service) {
        this.service = service;
    }

    /**
     * 开启存储。
     *
     * @param context
     * @param contactId
     * @param domain
     * @return
     */
    public boolean open(Context context, Long contactId, String domain) {
        if (null == this.sqlite) {
            this.contactId = contactId;
            this.domain = domain;
            this.sqlite = new SQLite(context);
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public void close() {
        if (null != this.sqlite) {
            this.sqlite.close();
            this.sqlite = null;
        }
    }

    /**
     * 读取联系人数据。
     *
     * @param contactId
     * @return
     */
    public Contact readContact(Long contactId) {
        Contact contact = null;
        SQLiteDatabase db = this.sqlite.getReadableDatabase();
        Cursor cursor = db.query("contact", new String[] { "name", "context", "timestamp" },
                "id=?", new String[] { contactId.toString() }, null, null, null);
        if (cursor.moveToFirst()) {
            String name = cursor.getString(0);
            String contextString = cursor.getString(1);
            long timestamp = cursor.getLong(2);
            // 实例化
            contact = new Contact(contactId, name, this.domain, timestamp);
            // 设置上下文数据
            if (contextString.length() > 3) {
                try {
                    JSONObject context = new JSONObject(contextString);
                    contact.setContext(context);
                } catch (JSONException e) {
                    // Nothing
                }
            }

            cursor.close();

            // 查找附录
            cursor = db.query("appendix", new String[] { "data" }, "id=?", new String[] { contactId.toString() },
                    null, null, null);
            if (cursor.moveToFirst()) {
                String data = cursor.getString(0);
                // 实例化附录
                try {
                    ContactAppendix appendix = new ContactAppendix(this.service, contact, new JSONObject(data));
                    contact.setAppendix(appendix);
                } catch (JSONException e) {
                    // Nothing
                }
            }

            cursor.close();
        }
        else {
            cursor.close();
        }

        db.close();
        return contact;
    }

    /**
     * 写入联系人数据。
     *
     * @param contact
     * @return
     */
    public boolean writeContact(Contact contact) {
        boolean result = true;

        String context = (null != contact.getContext()) ? contact.getContext().toString() : "";

        SQLiteDatabase db = this.sqlite.getWritableDatabase();
        Cursor cursor = db.query("contact", new String[] { "sn" },
                "id=?", new String[] { contact.id.toString() }, null, null, null);
        if (cursor.moveToFirst()) {
            cursor.close();

            // 更新数据
            ContentValues values = new ContentValues();
            values.put("name", contact.getName());
            values.put("context", context);
            values.put("timestamp", contact.getTimestamp());
            db.update("contact", values,
                    "id=?", new String[] { contact.id.toString() });

            // 更新附录
            ContactAppendix appendix = contact.getAppendix();
            if (null != appendix) {
                values = new ContentValues();
                values.put("data", appendix.toJSON().toString());
                int ret = db.update("appendix", values,
                        "id=?", new String[] { contact.id.toString() });
                if (ret == 0) {
                    // 尝试插入
                    values = new ContentValues();
                    values.put("id", contact.id.longValue());
                    values.put("data", appendix.toJSON().toString());
                    long tid = db.insert("appendix", null, values);
                    if (tid < 0) {
                        result = false;
                    }
                }
            }
        }
        else {
            cursor.close();

            // 插入数据
            ContentValues values = new ContentValues();
            values.put("id", contact.id.longValue());
            values.put("name", contact.getName());
            values.put("context", context);
            values.put("timestamp", contact.getTimestamp());
            db.insert("contact", null, values);

            // 插入附录
            ContactAppendix appendix = contact.getAppendix();
            if (null != appendix) {
                values = new ContentValues();
                values.put("id", contact.id.longValue());
                values.put("data", appendix.toJSON().toString());
                long tid = db.insert("appendix", null, values);
                if (tid < 0) {
                    result = false;
                }
            }
        }

        db.close();
        return result;
    }

    /**
     * 更新联系人的上下文数据。
     *
     * @param contactId
     * @param context
     */
    public void updateContactContext(Long contactId, JSONObject context) {
        SQLiteDatabase db = this.sqlite.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("context", context.toString());
        values.put("timestamp", System.currentTimeMillis());
        // 执行更新
        db.update("contact", values, "id=?", new String[] { contactId.toString() });

        db.close();
    }



    private class SQLite extends SQLiteOpenHelper {

        public SQLite(Context context) {
            super(context, "CubeContact_" + domain + "_" + contactId + ".db", null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            // 联系人表
            sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS `contact` (`sn` INTEGER PRIMARY KEY AUTOINCREMENT, `id` BIGINT, `name` TEXT, `context` TEXT, `timestamp` BIGINT)");

            // 群组表
            sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS `group` (`sn` INTEGER PRIMARY KEY AUTOINCREMENT, `id` BIGINT, `name` TEXT, `owner` TEXT, `tag` TEXT, `creation` BIGINT, `last_active` BIGINT, `state` INTEGER, `context` TEXT)");

            // 群成员表
            sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS `group_member` (`sn` INTEGER PRIMARY KEY AUTOINCREMENT, `group` BIGINT, `contact_id` BIGINT, `contact_name` TEXT, `contact_context` TEXT)");

            // 附录表
            sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS `appendix` (`id` BIGINT PRIMARY KEY, `data` TEXT)");

            // 联系人分区
            sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS `contact_zone` (`id` BIGINT PRIMARY KEY, `name` TEXT, `display_name` TEXT, `state` INTEGER, `timestamp` BIGINT)");

            // 联系人分区参与者
            sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS `contact_zone_participant` (`contact_zone_id` BIGINT, `contact_id` BIGINT, `state` INTEGER, `timestamp` BIGINT, `postscript` TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        }
    }
}