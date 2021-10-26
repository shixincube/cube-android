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

package cube.messaging;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import cube.core.Storage;
import cube.messaging.model.Message;

/**
 * 消息服务的存储器。
 */
public class MessagingStorage implements Storage {

    private final static int VERSION = 1;

    private MessagingService service;

    private String domain;

    private SQLite sqlite;

    public MessagingStorage(MessagingService service) {
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
            this.domain = domain;
            this.sqlite = new SQLite(context, contactId, domain);
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
     * 查询最近一条消息的时间戳。
     *
     * @return 返回本地最近一条消息的时间戳。
     */
    public long queryLastMessageTime() {
        long time = 0;

        SQLiteDatabase db = this.sqlite.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT `rts` FROM `message` WHERE `scope`=0 ORDER BY `rts` DESC LIMIT 1", new String[] {});
        if (cursor.moveToFirst()) {
            time = cursor.getLong(0);
        }
        cursor.close();
        db.close();

        return time;
    }

    /**
     * 更新消息数据。
     *
     * @param message
     * @return 返回数据库里是否已存在。
     */
    public boolean updateMessage(Message message) {
        boolean exists = false;

        SQLiteDatabase db = this.sqlite.getWritableDatabase();
        Cursor cursor = db.query("message", new String[]{ "id" },
                "id=?", new String[]{ message.id.toString() }, null, null, null);
        if (cursor.moveToFirst()) {
            cursor.close();

            // 更新消息状态
            ContentValues values = new ContentValues();
            values.put("rts", message.getRemoteTimestamp());
            values.put("state", message.getState().code);
            values.put("data", message.toJSON().toString());

            db.update("message", values, "id=?", new String[]{ message.id.toString() });

            exists = true;
        }
        else {
            cursor.close();

            // 插入消息数据
            ContentValues values = new ContentValues();
            values.put("id", message.id.longValue());
            values.put("from", message.getFrom());
            values.put("to", message.getTo());
            values.put("source", message.getSource());
            values.put("lts", message.getLocalTimestamp());
            values.put("rts", message.getRemoteTimestamp());
            values.put("state", message.getState().code);
            values.put("remote_state", message.getState().code);
            values.put("scope", message.getScope());
            values.put("data", message.toJSON().toString());

            db.insert("message", null, values);

            exists = false;
        }

        Long messagerId = 0L;

        // 更新最近消息
        if (message.isFromGroup()) {
            messagerId = message.getSource();
        }
        else {
            messagerId = message.getPartner().id;
        }

        cursor = db.query("recent_messager", new String[]{ "time" },
                "messager_id=?", new String[]{ messagerId.toString() }, null, null, null);
        if (cursor.moveToFirst()) {
            // 有记录，判断时间戳
            long time = cursor.getLong(0);

            cursor.close();

            if (message.getRemoteTimestamp() > time) {
                // 更新记录
                ContentValues values = new ContentValues();
                values.put("time", message.getRemoteTimestamp());
                values.put("message_id", message.getId().longValue());
                values.put("is_group", message.isFromGroup() ? 1 : 0);
                db.update("recent_messager", values,
                        "messager_id=?", new String[]{ messagerId.toString() });
            }
        }
        else {
            cursor.close();

            // 新记录
            ContentValues values = new ContentValues();
            values.put("messager_id", messagerId.longValue());
            values.put("time", message.getRemoteTimestamp());
            values.put("message_id", message.getId().longValue());
            values.put("is_group", message.isFromGroup() ? 1 : 0);
            db.insert("recent_messager", null, values);
        }

        db.close();

        return exists;
    }

    private class SQLite extends SQLiteOpenHelper {

        public SQLite(Context context, Long contactId, String domain) {
            super(context, "CubeMessaging_" + domain + "_" + contactId + ".db", null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            // 消息表
            sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS `message` (`id` BIGINT PRIMARY KEY, `from` BIGINT, `to` BIGINT, `source` BIGINT, `lts` BIGINT, `rts` BIGINT, `state` INT, `remote_state` INT DEFAULT 10, `scope` INT DEFAULT 0, `data` TEXT)");

            // 最近消息表，当前联系人和其他每一个联系人的最近消息
            // messager_id - 消息相关发件人或收件人 ID
            // time        - 消息时间戳
            // message_id  - 消息 ID
            // is_group    - 是否来自群组
            sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS `recent_messager` (`messager_id` BIGINT PRIMARY KEY, `time` BIGINT, `message_id` BIGINT, `is_group` INT)");

            // 消息草稿表
            // owner - 草稿对应的会话
            // time  - 草稿时间
            // data  - JSON 格式的数据
            sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS `draft` (`owner` BIGINT PRIMARY KEY, `time` BIGINT, `data` TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

        }
    }
}
