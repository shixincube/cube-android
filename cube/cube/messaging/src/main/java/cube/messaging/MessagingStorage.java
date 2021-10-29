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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cube.core.AbstractStorage;
import cube.messaging.model.Conversation;
import cube.messaging.model.ConversationReminded;
import cube.messaging.model.ConversationState;
import cube.messaging.model.ConversationType;
import cube.messaging.model.Message;

/**
 * 消息服务的存储器。
 */
public class MessagingStorage extends AbstractStorage {

    private final static int VERSION = 1;

    private MessagingService service;

    private String domain;

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
        if (null == this.sqliteHelper) {
            this.domain = domain;
            this.sqliteHelper = new SQLite(context, contactId, domain);
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public void close() {
        if (null != this.sqliteHelper) {
            this.sqliteHelper.close();
            this.sqliteHelper = null;
        }
    }

    /**
     * 查询最近的会话列表。
     *
     * @param limit 指定最大查询数量。
     * @return 返回查询结构数组。
     */
    public List<Conversation> queryRecentConversations(int limit) {
        List<Conversation> list = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        try {
            Cursor cursor = db.rawQuery("SELECT * FROM `conversation` WHERE `state`=? OR `state`=? ORDER BY `timestamp` DESC LIMIT ?",
                    new String[] { ConversationState.Normal.toString(),
                            ConversationState.Important.toString(), Integer.toString(limit)});
            while (cursor.moveToNext()) {
                String messageString = cursor.getString(cursor.getColumnIndex("recent_message"));
                Message recentMessage = new Message(new JSONObject(messageString), this.service);

                Conversation conversation = new Conversation(cursor.getLong(cursor.getColumnIndex("id")),
                        cursor.getLong(cursor.getColumnIndex("timestamp")),
                        ConversationType.parse(cursor.getInt(cursor.getColumnIndex("type"))),
                        ConversationState.parse(cursor.getInt(cursor.getColumnIndex("state"))),
                        cursor.getLong(cursor.getColumnIndex("pivotal_id")),
                        recentMessage,
                        ConversationReminded.parse(cursor.getInt(cursor.getColumnIndex("remind"))));
                // 填充实例
                this.service.fillConversation(conversation);

                list.add(conversation);
            }
        } catch (JSONException e) {
            // Nothing
        }

        this.closeReadableDatabase();

        return list;
    }

    /**
     * 更新列表里的所有会话。
     *
     * @param conversations 指定会话清单。
     */
    public void updateConversations(List<Conversation> conversations) {
        SQLiteDatabase db = this.getWritableDatabase();

        for (Conversation conversation : conversations) {
            Cursor cursor = db.query("conversation", new String[]{ "id" },
                    "id=?", new String[]{ conversation.id.toString() }, null, null, null);
            if (cursor.moveToFirst()) {
                cursor.close();

                // 更新
                ContentValues values = new ContentValues();
                values.put("timestamp", conversation.getTimestamp());
                values.put("state", conversation.getState().code);
                values.put("remind", conversation.getReminded().code);
                values.put("recent_message", conversation.getRecentMessage().toJSON().toString());
                db.update("conversation", values, "id=?", new String[]{ conversation.id.toString() });
            }
            else {
                cursor.close();

                // 插入
                ContentValues values = new ContentValues();
                values.put("id", conversation.id);
                values.put("timestamp", conversation.getTimestamp());
                values.put("type", conversation.getType().code);
                values.put("state", conversation.getState().code);
                values.put("pivotal_id", conversation.getPivotalId());
                values.put("remind", conversation.getReminded().code);
                values.put("recent_message", conversation.getRecentMessage().toJSON().toString());
                db.insert("conversation", null, values);
            }
        }

        this.closeWritableDatabase();
    }

    /**
     * 查询最近一条消息的时间戳。
     *
     * @return 返回本地最近一条消息的时间戳。
     */
    public long queryLastMessageTime() {
        long time = 0;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT `rts` FROM `message` WHERE `scope`=0 ORDER BY `rts` DESC LIMIT 1", new String[] {});
        if (cursor.moveToFirst()) {
            time = cursor.getLong(0);
        }
        cursor.close();
        this.closeReadableDatabase();

        return time;
    }

    /**
     * 查询最近消息列表。
     *
     * @param limit
     * @return
     */
    public List<Message> queryRecentMessages(int limit) {
        List<Message> list = new ArrayList<>();

        List<Long> messageIdList = new ArrayList<>();

        // 查询最近记录
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT `message_id` FROM `recent_messager` ORDER BY `time` DESC LIMIT ?",
                new String[]{ Integer.toString(limit) });
        while (cursor.moveToNext()) {
            Long messageId = cursor.getLong(0);
            messageIdList.add(messageId);
        }
        cursor.close();

        // 查询消息
        for (Long messageId : messageIdList) {
            cursor = db.query("message", new String[]{ "data" },
                    "id=?", new String[]{ messageId.toString() }, null, null, null);
            if (cursor.moveToFirst()) {
                String dataString = cursor.getString(0);
                try {
                    JSONObject json = new JSONObject(dataString);
                    Message message = new Message(json, this.service);

                    // 填充数据
                    this.service.fillMessage(message);

                    list.add(message);
                } catch (JSONException e) {
                    // Nothing
                }
            }
            cursor.close();
        }

        this.closeReadableDatabase();

        return list;
    }

    /**
     * 更新消息数据。
     *
     * @param message
     * @return 返回数据库里是否已存在。
     */
    public boolean updateMessage(Message message) {
        boolean exists = false;

        SQLiteDatabase db = this.getWritableDatabase();
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
            values.put("`from`", message.getFrom());
            values.put("`to`", message.getTo());
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
            messagerId = message.isSelfTyper() ? message.getTo() : message.getFrom();
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

        this.closeWritableDatabase();

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

            // 会话表
            sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS `conversation` (`id` BIGINT PRIMARY KEY, `timestamp` BIGINT, `type` INT, `state` INT, `pivotal_id` BIGINT, `remind` INT, `recent_message` TEXT, `avatar_name` TEXT DEFAULT NULL, `avatar_url` TEXT DEFAULT NULL)");

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
