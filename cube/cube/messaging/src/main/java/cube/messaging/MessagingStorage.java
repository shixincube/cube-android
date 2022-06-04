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
import android.util.MutableBoolean;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cube.core.AbstractStorage;
import cube.messaging.model.Conversation;
import cube.messaging.model.ConversationReminding;
import cube.messaging.model.ConversationState;
import cube.messaging.model.ConversationType;
import cube.messaging.model.FileAttachment;
import cube.messaging.model.Message;
import cube.messaging.model.MessageScope;
import cube.messaging.model.MessageState;

/**
 * 消息服务的存储器。
 */
public class MessagingStorage extends AbstractStorage {

    private final static int VERSION = 1;

    private MessagingService service;

    private String domain;

    public MessagingStorage(MessagingService service) {
        super();
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
    public void open(Context context, Long contactId, String domain) {
        super.open(context, "CubeMessaging_" + domain + "_" + contactId + ".db", VERSION);
        this.domain = domain;
    }

    /**
     * 清空所有表数据。
     */
    public void cleanup() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("message", null, null);
        db.delete("conversation", null, null);
        db.delete("recent_messager", null, null);
        db.delete("draft", null, null);
        this.closeWritableDatabase(db);
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

        Cursor cursor = db.rawQuery("SELECT * FROM `conversation` WHERE `state`=? OR `state`=? ORDER BY `timestamp` DESC LIMIT ?",
                new String[] { ConversationState.Normal.toString(),
                        ConversationState.Important.toString(),
                        Integer.toString(limit) });
        while (cursor.moveToNext()) {
            String messageString = cursor.getString(cursor.getColumnIndex("recent_message"));
            Message recentMessage = null;
            try {
                recentMessage = new Message(this.service, new JSONObject(messageString));
                if (recentMessage.isEmpty()) {
                    // 消息是空白
                    continue;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                continue;
            }

            Conversation conversation = new Conversation(cursor.getLong(cursor.getColumnIndex("id")),
                    cursor.getLong(cursor.getColumnIndex("timestamp")),
                    ConversationType.parse(cursor.getInt(cursor.getColumnIndex("type"))),
                    ConversationState.parse(cursor.getInt(cursor.getColumnIndex("state"))),
                    cursor.getLong(cursor.getColumnIndex("pivotal_id")),
                    ConversationReminding.parse(cursor.getInt(cursor.getColumnIndex("reminding"))),
                    cursor.getInt(cursor.getColumnIndex("unread")),
                    recentMessage);
            // 添加到列表
            list.add(conversation);
        }

        cursor.close();
        this.closeReadableDatabase(db);

        for (Conversation conversation : list) {
            Message message = null;
            if (conversation.getType() == ConversationType.Contact) {
                message = this.queryLastMessageNoFillByContactId(conversation.getPivotalId(), true);
            }
            else if (conversation.getType() == ConversationType.Group) {
                message = this.queryLastMessageNoFillByGroupId(conversation.getPivotalId(), true);
            }

            if (null != message) {
                conversation.setRecentMessage(message);
            }

            // 填充实例
            this.service.fillConversation(conversation);
        }

        return list;
    }

    /**
     * 获取最近一次更新的会话。
     *
     * @return
     */
    public Conversation getLastConversation() {
        Conversation conversation = null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM `conversation` WHERE `state`=? OR `state`=? ORDER BY `timestamp` DESC LIMIT 1",
                new String[] { ConversationState.Normal.toString(), ConversationState.Important.toString() });
        if (cursor.moveToFirst()) {
            try {
                String messageString = cursor.getString(cursor.getColumnIndex("recent_message"));
                Message recentMessage = new Message(this.service, new JSONObject(messageString));

                conversation = new Conversation(cursor.getLong(cursor.getColumnIndex("id")),
                        cursor.getLong(cursor.getColumnIndex("timestamp")),
                        ConversationType.parse(cursor.getInt(cursor.getColumnIndex("type"))),
                        ConversationState.parse(cursor.getInt(cursor.getColumnIndex("state"))),
                        cursor.getLong(cursor.getColumnIndex("pivotal_id")),
                        ConversationReminding.parse(cursor.getInt(cursor.getColumnIndex("reminding"))),
                        cursor.getInt(cursor.getColumnIndex("unread")),
                        recentMessage);
            } catch (JSONException e) {
                // Nothing;
            }
        }
        cursor.close();
        this.closeReadableDatabase(db);

        if (null != conversation) {
            Message message = null;
            if (conversation.getType() == ConversationType.Contact) {
                message = this.queryLastMessageNoFillByContactId(conversation.getPivotalId(), true);
            }
            else if (conversation.getType() == ConversationType.Group) {
                message = this.queryLastMessageNoFillByGroupId(conversation.getPivotalId(), true);
            }

            if (null != message) {
                conversation.setRecentMessage(message);
            }

            // 填充实例
            this.service.fillConversation(conversation);
        }

        return conversation;
    }

    /**
     * 读取指定 ID 的会话。
     *
     * @param conversationId
     * @return
     */
    public Conversation readConversation(Long conversationId) {
        Conversation conversation = null;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM `conversation` WHERE `id`=? AND `state`<>?",
                new String[] { conversationId.toString(), ConversationState.Destroyed.toString() });
        if (cursor.moveToFirst()) {
            try {
                String messageString = cursor.getString(cursor.getColumnIndex("recent_message"));
                Message recentMessage = new Message(this.service, new JSONObject(messageString));

                conversation = new Conversation(cursor.getLong(cursor.getColumnIndex("id")),
                        cursor.getLong(cursor.getColumnIndex("timestamp")),
                        ConversationType.parse(cursor.getInt(cursor.getColumnIndex("type"))),
                        ConversationState.parse(cursor.getInt(cursor.getColumnIndex("state"))),
                        cursor.getLong(cursor.getColumnIndex("pivotal_id")),
                        ConversationReminding.parse(cursor.getInt(cursor.getColumnIndex("reminding"))),
                        cursor.getInt(cursor.getColumnIndex("unread")),
                        recentMessage);
            } catch (Exception e) {
                // Nothing
            }
        }
        cursor.close();
        this.closeReadableDatabase(db);

        // 读取最近消息
        if (null != conversation) {
            Message message = null;
            if (conversation.getType() == ConversationType.Contact) {
                message = this.queryLastMessageNoFillByContactId(conversation.getPivotalId(), true);
            }
            else if (conversation.getType() == ConversationType.Group) {
                message = this.queryLastMessageNoFillByGroupId(conversation.getPivotalId(), true);
            }

            if (null != message) {
                conversation.setRecentMessage(message);
            }

            // 填充实例
            this.service.fillConversation(conversation);
        }
        return conversation;
    }

    /**
     * 写入新的会话数据。
     *
     * @param conversation
     * @return 返回 {@code true} 表示插入新数据。
     */
    public boolean writeConversation(Conversation conversation) {
        boolean insert = false;
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query("conversation", new String[]{ "id" },
                "id=?", new String[]{ conversation.id.toString() }, null, null, null);
        if (cursor.moveToFirst()) {
            cursor.close();

            // 更新
            ContentValues values = new ContentValues();
            values.put("timestamp", conversation.getTimestamp());
            values.put("state", conversation.getState().code);
            values.put("reminding", conversation.getReminding().code);
            values.put("recent_message", conversation.getRecentMessage().toJSON().toString());
            values.put("unread", conversation.getUnreadCount());
            // update
            db.update("conversation", values, "id=?", new String[]{ conversation.id.toString() });
        }
        else {
            cursor.close();
            insert = true;

            // 插入
            ContentValues values = new ContentValues();
            values.put("id", conversation.id);
            values.put("timestamp", conversation.getTimestamp());
            values.put("type", conversation.getType().code);
            values.put("state", conversation.getState().code);
            values.put("pivotal_id", conversation.getPivotalId());
            values.put("reminding", conversation.getReminding().code);
            values.put("recent_message", conversation.getRecentMessage().toJSON().toString());
            values.put("unread", conversation.getUnreadCount());
            // insert
            db.insert("conversation", null, values);
        }

        this.closeWritableDatabase(db);

        return insert;
    }

    /**
     * 删除会话以及相关的所有消息记录。
     *
     * @param conversation
     */
    public void deleteConversationAndMessages(Conversation conversation) {
        SQLiteDatabase db = this.getWritableDatabase();

        // 删除会话
        db.delete("conversation", "id=?", new String[]{ conversation.id.toString() });

        // 删除消息
        if (conversation.getType() == ConversationType.Contact) {
            db.delete("message", "`source`=0 AND (`from`=? OR `to`=?)",
                    new String[]{
                            conversation.getPivotalId().toString(),
                            conversation.getPivotalId().toString()
                    });
        }
        else if (conversation.getType() == ConversationType.Group) {
            db.delete("message", "`source`=?",
                    new String[]{
                            conversation.getPivotalId().toString()
                    });
        }

        this.closeWritableDatabase(db);
    }

    /**
     * 更新会话。
     *
     * @param conversation
     */
    public void updateConversation(Conversation conversation) {
        SQLiteDatabase db = this.getWritableDatabase();

        // 更新
        ContentValues values = new ContentValues();
        values.put("timestamp", conversation.getTimestamp());
        values.put("state", conversation.getState().code);
        values.put("reminding", conversation.getReminding().code);
        values.put("recent_message", conversation.getRecentMessage().toJSON().toString());
        values.put("unread", conversation.getUnreadCount());
        // update
        db.update("conversation", values, "id=?", new String[]{ conversation.id.toString() });

        this.closeWritableDatabase(db);
    }

    /**
     * 仅更新会话的时间戳。
     *
     * @param conversation
     */
    public void updateConversationTimestamp(Conversation conversation) {
        SQLiteDatabase db = this.getWritableDatabase();

        // 更新
        ContentValues values = new ContentValues();
        values.put("timestamp", conversation.getTimestamp());
        db.update("conversation", values, "id=?", new String[]{ conversation.id.toString() });

        this.closeWritableDatabase(db);
    }

    /**
     * 更新列表里的所有会话。
     *
     * @param conversations 指定会话清单。
     */
    public void updateConversations(List<Conversation> conversations) {
        SQLiteDatabase db = this.getWritableDatabase();

        for (Conversation conversation : conversations) {
            if (conversation.getState() == ConversationState.Deleted) {
                // 跳过已删除的会话
                continue;
            }

            Cursor cursor = db.query("conversation", new String[]{ "id" },
                    "id=?", new String[]{ conversation.id.toString() }, null, null, null);
            if (cursor.moveToFirst()) {
                cursor.close();

                // 更新
                ContentValues values = new ContentValues();
                values.put("timestamp", conversation.getTimestamp());
                values.put("state", conversation.getState().code);
                values.put("reminding", conversation.getReminding().code);
                values.put("recent_message", conversation.getRecentMessage().toJSON().toString());
                values.put("unread", conversation.getUnreadCount());
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
                values.put("reminding", conversation.getReminding().code);
                values.put("recent_message", conversation.getRecentMessage().toJSON().toString());
                values.put("unread", conversation.getUnreadCount());
                db.insert("conversation", null, values);
            }
        }

        this.closeWritableDatabase(db);
    }

    /**
     * 更新指定会的最近消息。
     *
     * @param conversation
     */
    public void updateRecentMessage(Conversation conversation) {
        SQLiteDatabase db = this.getWritableDatabase();

        Message message = conversation.getRecentMessage();

        boolean unread = !message.isSelfTyper();

        Cursor cursor = db.query("conversation", new String[]{ "type" },
                "id=?", new String[]{ conversation.id.toString() }, null, null, null);
        if (cursor.moveToFirst()) {
            cursor.close();

            String sql = "UPDATE `conversation` SET `timestamp`=" + conversation.getTimestamp()
                    + " , `recent_message`=? "
                    + (unread ? ", `unread`=`unread`+1 " : " ")
                    + "WHERE `id`=?";

            db.execSQL(sql, new String[] {
                    message.toJSON().toString(),
                    conversation.id.toString()
            });
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
            values.put("reminding", conversation.getReminding().code);
            values.put("recent_message", message.toJSON().toString());
            values.put("unread", unread ? 1 : 0);
            db.insert("conversation", null, values);
        }

        this.closeWritableDatabase(db);
    }

    protected Message readMessageNoFillById(Long messageId) {
        Message message = null;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM `message` WHERE `id`=?",
                new String[]{ messageId.toString() });
        if (cursor.moveToFirst()) {
            message = this.readMessage(cursor);
        }
        cursor.close();
        this.closeReadableDatabase(db);

        return message;
    }

    /**
     * 更新指定会话关联的所有消息的状态。
     *
     * @param conversation
     * @param currentState
     * @param newState
     * @return
     */
    public List<Long> updateMessageState(Conversation conversation, MessageState currentState, MessageState newState) {
        List<Long> idList = new ArrayList<>();

        SQLiteDatabase db = this.getWritableDatabase();

        if (conversation.getType() == ConversationType.Contact) {
            // 查找符合条件的数据
            Cursor cursor = db.query("message", new String[]{ "id" },
                    "source=0 AND scope=0 AND state=? AND `from`=?",
                    new String[]{
                            currentState.toString(),
                            conversation.getPivotalId().toString()
                    }, null, null, null);
            while (cursor.moveToNext()) {
                Long id = cursor.getLong(0);
                idList.add(id);
            }
            cursor.close();

            ContentValues values = new ContentValues();
            values.put("state", newState.code);
            db.update("message", values, "`source`=0 AND scope=0 AND state=? AND `from`=?",
                    new String[] {
                            currentState.toString(),
                            conversation.getPivotalId().toString()});
        }
        else if (conversation.getType() == ConversationType.Group) {
            // 查找符合条件的数据
            Cursor cursor = db.query("message", new String[]{ "id" },
                    "scope=0 AND state=? AND source=?",
                    new String[]{
                            currentState.toString(),
                            conversation.getPivotalId().toString()
                    }, null, null, null);
            while (cursor.moveToNext()) {
                Long id = cursor.getLong(0);
                idList.add(id);
            }
            cursor.close();

            ContentValues values = new ContentValues();
            values.put("state", newState.code);
            db.update("message", values, "scope=0 AND state=? AND source=?",
                    new String[] {
                            currentState.toString(),
                            conversation.getPivotalId().toString()});
        }

        this.closeWritableDatabase(db);
        return idList;
    }

    /**
     * 查询最近一条消息的时间戳。
     *
     * @return 返回本地最近一条消息的时间戳。
     */
    public long queryLastMessageTime() {
        long time = 0;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT `rts` FROM `message` WHERE `scope`=? ORDER BY `rts` DESC LIMIT 1",
                new String[]{ Integer.toString(MessageScope.Unlimited) });
        if (cursor.moveToFirst()) {
            time = cursor.getLong(0);
        }
        cursor.close();
        this.closeReadableDatabase(db);

        return time;
    }

    public void updateMessagesRemoteState(List<Long> messageIdList, MessageState state) {
        SQLiteDatabase db = this.getWritableDatabase();
        for (Long id : messageIdList) {
            ContentValues values = new ContentValues();
            values.put("remote_state", state.code);
            // update
            db.update("message", values, "id=?", new String[] {
                    id.toString()
            });
        }
        this.closeWritableDatabase(db);
    }

    public boolean updateMessageAttachment(Long messageId, FileAttachment attachment) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("attachment", attachment.toJSON().toString());
        // update
        int row = db.update("message", values, "id=?", new String[] {
                messageId.toString()
        });

        this.closeWritableDatabase(db);

        return row > 0;
    }

    /**
     * 更新指定 ID 消息的状态。
     *
     * @param messageId
     * @param currentState
     * @param newState
     */
    public void updateMessage(Long messageId, MessageState currentState, MessageState newState) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("state", newState.code);
        db.update("message", values, "id=? AND state=?", new String[]{
                messageId.toString(),
                currentState.toString()
        });
        this.closeWritableDatabase(db);
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
            values.put("payload", message.getPayload().toString());

            if (null != message.getAttachment()) {
                values.put("attachment", message.getAttachment().toJSON().toString());
            }

            db.update("message", values, "id=?", new String[]{ message.id.toString() });

            exists = true;
        }
        else {
            cursor.close();

            // 插入消息数据
            ContentValues values = new ContentValues();
            values.put("id", message.id.longValue());
            values.put("timestamp", message.getTimestamp());
            values.put("owner", message.getOwner());
            values.put("`from`", message.getFrom());
            values.put("`to`", message.getTo());
            values.put("source", message.getSource());
            values.put("lts", message.getLocalTimestamp());
            values.put("rts", message.getRemoteTimestamp());
            values.put("state", message.getState().code);
            values.put("remote_state", message.getState().code);
            values.put("scope", message.getScope());
            values.put("payload", message.getPayload().toString());

            if (null != message.getAttachment()) {
                values.put("attachment", message.getAttachment().toJSON().toString());
            }

            db.insert("message", null, values);

            exists = false;
        }

        Long messagerId = 0L;

        // 更新最近消息
        if (message.isFromGroup()) {
            messagerId = message.getSource();
        }
        else {
            messagerId = message.getPartnerId();
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

        this.closeWritableDatabase(db);

        return exists;
    }

    /**
     * 反向查询消息。
     *
     * @param contactId
     * @param timestamp
     * @param limit
     * @return
     */
    protected MessageListResult queryMessagesByReverseWithContact(Long contactId, long timestamp, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();

        final List<Message> messageList = new ArrayList<>();
        final MutableBoolean hasMore = new MutableBoolean(false);

        // 查询消息记录，这里比 limit 多查一条记录以判断是否还有更多消息。
        Cursor cursor = db.rawQuery("SELECT * FROM `message` WHERE (`scope`=? OR `scope`=?) AND `rts`<? AND (`from`=? OR `to`=?) AND `source`=0 AND (`state`=? OR `state`=? OR `state`=?) ORDER BY `rts` DESC LIMIT ?"
            , new String[] {
                        Integer.toString(MessageScope.Unlimited),
                        Integer.toString(MessageScope.Private),
                        Long.toString(timestamp),
                        contactId.toString(),
                        contactId.toString(),
                        MessageState.Sending.toString(),
                        MessageState.Sent.toString(),
                        MessageState.Read.toString(),
                        Integer.toString(limit + 1)
                });

        while (cursor.moveToNext()) {
            if (messageList.size() == limit) {
                hasMore.value = true;
                break;
            }

            Message message = readMessage(cursor);

            // 填充
            message = this.service.fillMessage(message);
            messageList.add(message);
        }

        cursor.close();
        this.closeReadableDatabase(db);

        return new MessageListResult() {

            @Override
            public List<Message> getList() {
                return messageList;
            }

            @Override
            public boolean hasMore() {
                return hasMore.value;
            }
        };
    }

    /**
     * 反向查询消息。
     *
     * @param groupId
     * @param timestamp
     * @param limit
     * @return
     */
    protected MessageListResult queryMessagesByReverseWithGroup(Long groupId, long timestamp, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();

        final List<Message> messageList = new ArrayList<>();
        final MutableBoolean hasMore = new MutableBoolean(false);

        // 查询消息记录，这里比 limit 多查一条记录以判断是否还有更多消息。
        Cursor cursor = db.rawQuery("SELECT * FROM `message` WHERE (`scope`=? OR `scope`=?) AND `rts`<? AND `source`=? AND (`state`=? OR `state`=? OR `state`=?) ORDER BY `rts` DESC LIMIT ?"
                , new String[] {
                        Integer.toString(MessageScope.Unlimited),
                        Integer.toString(MessageScope.Private),
                        Long.toString(timestamp),
                        groupId.toString(),
                        MessageState.Sending.toString(),
                        MessageState.Sent.toString(),
                        MessageState.Read.toString(),
                        Integer.toString(limit + 1)
                });

        while (cursor.moveToNext()) {
            if (messageList.size() == limit) {
                hasMore.value = true;
                break;
            }

            Message message = this.readMessage(cursor);

            // 填充
            message = this.service.fillMessage(message);
            messageList.add(message);
        }

        cursor.close();
        this.closeReadableDatabase(db);

        return new MessageListResult() {

            @Override
            public List<Message> getList() {
                return messageList;
            }

            @Override
            public boolean hasMore() {
                return hasMore.value;
            }
        };
    }

    private Message queryLastMessageNoFillByContactId(Long contactId, boolean onlyUnlimited) {
        Message message = null;
        SQLiteDatabase db = this.getReadableDatabase();

        String scopeCondition = null;
        if (onlyUnlimited) {
            scopeCondition = String.format(Locale.ROOT, "(`scope`=%d)", MessageScope.Unlimited);
        }
        else {
            scopeCondition = String.format(Locale.ROOT, "(`scope`=%d OR `scope`=%d)", MessageScope.Unlimited, MessageScope.Private);
        }

        Cursor cursor = db.rawQuery("SELECT * FROM `message` WHERE " + scopeCondition + " AND (`from`=? OR `to`=?) AND `source`=0 AND (`state`=? OR `state`=? OR `state`=?) ORDER BY `rts` DESC LIMIT 1", new String[] {
                contactId.toString(),
                contactId.toString(),
                MessageState.Read.toString(),
                MessageState.Sent.toString(),
                MessageState.Sending.toString()
        });

        if (cursor.moveToFirst()) {
            message = this.readMessage(cursor);
        }
        cursor.close();
        this.closeReadableDatabase(db);
        return message;
    }

    private Message queryLastMessageNoFillByGroupId(Long groupId, boolean onlyUnlimited) {
        Message message = null;
        SQLiteDatabase db = this.getReadableDatabase();

        String scopeCondition = null;
        if (onlyUnlimited) {
            scopeCondition = String.format(Locale.ROOT, "(`scope`=%d)", MessageScope.Unlimited);
        }
        else {
            scopeCondition = String.format(Locale.ROOT, "(`scope`=%d OR `scope`=%d)", MessageScope.Unlimited, MessageScope.Private);
        }

        Cursor cursor = db.rawQuery("SELECT * FROM `message` WHERE " + scopeCondition + " AND `source`=? AND (`state`=? OR `state`=? OR `state`=?) ORDER BY `rts` DESC LIMIT 1", new String[] {
                groupId.toString(),
                MessageState.Read.toString(),
                MessageState.Sent.toString(),
                MessageState.Sending.toString()
        });

        if (cursor.moveToFirst()) {
            message = this.readMessage(cursor);
        }
        cursor.close();
        this.closeReadableDatabase(db);
        return message;
    }

    private Message readMessage(Cursor cursor) {
        String payloadString = cursor.getString(cursor.getColumnIndex("payload"));
        JSONObject payload = null;
        try {
            payload = new JSONObject(payloadString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        FileAttachment attachment = null;
        String attachmentString = cursor.getString(cursor.getColumnIndex("attachment"));
        if (null != attachmentString && attachmentString.length() > 3) {
            try {
                attachment = new FileAttachment(new JSONObject(attachmentString));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Message message = new Message(cursor.getLong(cursor.getColumnIndex("id")),
                cursor.getLong(cursor.getColumnIndex("timestamp")),
                this.domain,
                cursor.getLong(cursor.getColumnIndex("owner")),
                cursor.getLong(cursor.getColumnIndex("from")),
                cursor.getLong(cursor.getColumnIndex("to")),
                cursor.getLong(cursor.getColumnIndex("source")),
                cursor.getLong(cursor.getColumnIndex("lts")),
                cursor.getLong(cursor.getColumnIndex("rts")),
                payload,
                MessageState.parse(cursor.getInt(cursor.getColumnIndex("state"))),
                cursor.getInt(cursor.getColumnIndex("scope")),
                attachment);
        return message;
    }

    @Override
    protected void onDatabaseCreate(SQLiteDatabase database) {
        // 消息表
        database.execSQL("CREATE TABLE IF NOT EXISTS `message` (`id` BIGINT PRIMARY KEY, `timestamp` BIGINT, `owner` BIGINT, `from` BIGINT, `to` BIGINT, `source` BIGINT, `lts` BIGINT, `rts` BIGINT, `state` INT, `remote_state` INT DEFAULT 10, `scope` INT DEFAULT 0, `payload` TEXT, `attachment` TEXT DEFAULT NULL)");

        // 会话表
        database.execSQL("CREATE TABLE IF NOT EXISTS `conversation` (`id` BIGINT PRIMARY KEY, `timestamp` BIGINT, `type` INT, `state` INT, `pivotal_id` BIGINT, `reminding` INT, `recent_message` TEXT, `unread` INT DEFAULT 0, `avatar_name` TEXT DEFAULT NULL, `avatar_url` TEXT DEFAULT NULL)");

        // 最近消息表，当前联系人和其他每一个联系人的最近消息
        // messager_id - 消息相关发件人或收件人 ID
        // time        - 消息时间戳
        // message_id  - 消息 ID
        // is_group    - 是否来自群组
        database.execSQL("CREATE TABLE IF NOT EXISTS `recent_messager` (`messager_id` BIGINT PRIMARY KEY, `time` BIGINT, `message_id` BIGINT, `is_group` INT)");

        // 消息草稿表
        // owner - 草稿对应的会话
        // time  - 草稿时间
        // data  - JSON 格式的数据
        database.execSQL("CREATE TABLE IF NOT EXISTS `draft` (`owner` BIGINT PRIMARY KEY, `time` BIGINT, `data` TEXT)");
    }

    @Override
    protected void onDatabaseUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {

    }
}
