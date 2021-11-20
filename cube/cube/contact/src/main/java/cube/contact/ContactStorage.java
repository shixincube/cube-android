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

package cube.contact;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import cube.contact.model.Contact;
import cube.contact.model.ContactAppendix;
import cube.contact.model.ContactZone;
import cube.contact.model.ContactZoneParticipant;
import cube.contact.model.ContactZoneParticipantState;
import cube.contact.model.ContactZoneState;
import cube.contact.model.Group;
import cube.contact.model.GroupAppendix;
import cube.contact.model.GroupState;
import cube.core.AbstractStorage;
import cube.core.model.Entity;

/**
 * 联系人模块存储器。
 */
public class ContactStorage extends AbstractStorage {

    private final static int VERSION = 1;

    private ContactService service;

    private String domain;

    public ContactStorage(ContactService service) {
        super();
        this.service = service;
    }

    /**
     * 开启存储。
     *
     * @param context
     * @param contactId
     * @param domain
     */
    public void open(Context context, Long contactId, String domain) {
        super.open(context, "CubeContact_" + domain + "_" + contactId + ".db", VERSION);
        this.domain = domain;
    }

    /**
     * 读取联系人数据。
     *
     * @param contactId
     * @return
     */
    public Contact readContact(Long contactId) {
        Contact contact = null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("contact", new String[] { "name", "context", "timestamp", "last", "expiry" },
                "id=?", new String[] { contactId.toString() }, null, null, null);
        if (cursor.moveToFirst()) {
            String name = cursor.getString(0);
            String contextString = cursor.getString(1);
            long timestamp = cursor.getLong(2);
            long last = cursor.getLong(3);
            long expiry = cursor.getLong(4);
            // 实例化
            contact = new Contact(contactId, name, this.domain, timestamp);
            // 重置时间戳
            contact.resetExpiry(expiry, last);
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

        this.closeReadableDatabase(db);
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

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query("contact", new String[] { "sn" },
                "id=?", new String[] { contact.id.toString() }, null, null, null);
        if (cursor.moveToFirst()) {
            cursor.close();

            // 更新数据
            ContentValues values = new ContentValues();
            values.put("name", contact.getName());
            values.put("context", context);
            values.put("timestamp", contact.getTimestamp());
            values.put("last", contact.getLast());
            values.put("expiry", contact.getExpiry());
            db.update("contact", values,
                    "id=?", new String[] { contact.id.toString() });
        }
        else {
            cursor.close();

            // 插入数据
            ContentValues values = new ContentValues();
            values.put("id", contact.id);
            values.put("name", contact.getName());
            values.put("context", context);
            values.put("timestamp", contact.getTimestamp());
            values.put("last", contact.getLast());
            values.put("expiry", contact.getExpiry());
            db.insert("contact", null, values);
        }

        // 更新附录
        ContactAppendix appendix = contact.getAppendix();
        if (null != appendix) {
            ContentValues values = new ContentValues();
            values.put("data", appendix.toJSON().toString());
            values.put("timestamp", contact.getLast());
            int ret = db.update("appendix", values,
                    "id=?", new String[] { contact.id.toString() });
            if (ret == 0) {
                // 尝试插入
                values = new ContentValues();
                values.put("id", contact.id);
                values.put("data", appendix.toJSON().toString());
                values.put("timestamp", contact.getLast());
                long tid = db.insert("appendix", null, values);
                if (tid < 0) {
                    result = false;
                }
            }
        }

        this.closeWritableDatabase(db);
        return result;
    }

    /**
     * 更新联系人名称。
     *
     * @param contact
     */
    public void updateContactName(Contact contact) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", contact.getName());
        values.put("last", contact.getLast());
        values.put("expiry", contact.getExpiry());
        // update
        db.update("contact", values,
                "id=?", new String[] { contact.id.toString() });

        this.closeWritableDatabase(db);
    }

    /**
     * 更新联系人的上下文数据。
     *
     * @param contact
     * @return 返回当前更新的时间戳。
     */
    public long updateContactContext(Contact contact) {
        SQLiteDatabase db = this.getWritableDatabase();

        long now = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put("context", contact.getContext().toString());
        values.put("last", now);
        values.put("expiry", now + Entity.LIFESPAN_IN_MSEC);
        // 执行更新
        int row = db.update("contact", values, "id=?", new String[] { contact.id.toString() });

        if (row == 0) {
            // 插入数据
            this.service.execute(new Runnable() {
                @Override
                public void run() {
                    writeContact(contact);
                }
            });
        }

        this.closeWritableDatabase(db);

        return now;
    }

    public void clearAllContactContexts() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("context", "");
        db.update("contact", values, null, null);
        this.closeWritableDatabase(db);
    }

    /**
     * 获取最近更新的群组的时间戳。
     *
     * @return
     */
    public long queryLastGroupActiveTime() {
        long timestamp = 0;

        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT `last_active` FROM `group` ORDER BY `last_active` DESC LIMIT 1";
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            timestamp = cursor.getLong(0);
        }
        cursor.close();
        this.closeReadableDatabase(db);

        return timestamp;
    }

    /**
     * 读取群组
     * @param groupId
     * @return
     */
    public Group readGroup(Long groupId) {
        Group group = null;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("`group`", new String[]{ "*" },
                "`id`=?", new String[]{ groupId.toString() }, null, null, null);
        if (cursor.moveToFirst()) {
            // 实例化
            group = this.readGroup(cursor);
        }
        cursor.close();

        if (null != group) {
            // 读取成员列表
            cursor = db.query("group_member", new String[]{ "contact_id" },
                    "`group`=?", new String[]{ groupId.toString() }, null, null, null);
            while (cursor.moveToNext()) {
                Long memberId = cursor.getLong(0);
                group.addMember(memberId);
            }
            cursor.close();

            // 读取附录
            cursor = db.query("appendix", new String[]{ "data" },
                    "`id`=?", new String[]{ groupId.toString() }, null, null, null);
            if (cursor.moveToFirst()) {
                String string = cursor.getString(0);
                try {
                    GroupAppendix appendix = new GroupAppendix(this.service, group, new JSONObject(string));
                    group.setAppendix(appendix);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            cursor.close();
        }

        this.closeReadableDatabase(db);

        return group;
    }

    /**
     * 写入群组数据。
     *
     * @param group
     */
    public void writeGroup(Group group) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", group.getName());
        values.put("owner", group.getOwnerId());
        values.put("tag", group.getTag());
        values.put("creation", group.getCreationTime());
        values.put("last_active", group.getLastActive());
        values.put("state", group.getState().code);
        values.put("last", group.getLast());
        values.put("expiry", group.getExpiry());
        if (null != group.getContext()) {
            values.put("context", group.getContext().toString());
        }

        Cursor cursor = db.query("`group`", new String[]{ "sn" },
                "id=?", new String[]{ group.id.toString() }, null, null, null);
        if (cursor.moveToFirst()) {
            cursor.close();

            // 更新
            db.update("`group`", values, "id=?", new String[]{ group.id.toString() });
        }
        else {
            cursor.close();

            // 插入
            values.put("id", group.id);
            db.insert("`group`", null, values);
        }

        // 处理成员列表
        for (Long memberId : group.getMemberIdList()) {
            cursor = db.query("group_member", new String[]{ "sn" },
                    "`group`=? AND contact_id=?", new String[]{ group.id.toString(), memberId.toString() },
                    null, null, null);
            if (cursor.moveToFirst()) {
                cursor.close();
            }
            else {
                cursor.close();

                // 插入数据
                ContentValues member = new ContentValues();
                member.put("`group`", group.id);
                member.put("contact_id", memberId);
                member.put("timestamp", group.getTimestamp());
                db.insert("group_member", null, member);
            }
        }

        this.closeWritableDatabase(db);

        // 写入附录
        GroupAppendix appendix = group.getAppendix();
        if (null != appendix) {
            this.writeAppendix(appendix);
        }
    }

    /**
     * 写入联系人附录。
     *
     * @param appendix
     */
    public synchronized void writeAppendix(ContactAppendix appendix) {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query("appendix", new String[] { "id" },
                "id=?", new String[] { appendix.getOwner().id.toString() }, null, null, null);
        if (cursor.moveToFirst()) {
            cursor.close();

            // 更新
            ContentValues values = new ContentValues();
            values.put("timestamp", System.currentTimeMillis());
            values.put("data", appendix.toJSON().toString());
            db.update("appendix", values, "id=?", new String[] { appendix.getOwner().id.toString() });
        }
        else {
            cursor.close();

            // 插入
            ContentValues values = new ContentValues();
            values.put("id", appendix.getOwner().id);
            values.put("timestamp", System.currentTimeMillis());
            values.put("data", appendix.toJSON().toString());
            db.insert("appendix", null, values);
        }

        this.closeWritableDatabase(db);
    }

    /**
     * 写入群组附录。
     *
     * @param appendix
     */
    public synchronized void writeAppendix(GroupAppendix appendix) {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query("appendix", new String[] { "id" },
                "id=?", new String[] { appendix.getOwner().id.toString() }, null, null, null);
        if (cursor.moveToFirst()) {
            cursor.close();

            // 更新
            ContentValues values = new ContentValues();
            values.put("timestamp", System.currentTimeMillis());
            values.put("data", appendix.toJSON().toString());
            db.update("appendix", values, "id=?", new String[] { appendix.getOwner().id.toString() });
        }
        else {
            cursor.close();

            // 插入
            ContentValues values = new ContentValues();
            values.put("id", appendix.getOwner().id);
            values.put("timestamp", System.currentTimeMillis());
            values.put("data", appendix.toJSON().toString());
            db.insert("appendix", null, values);
        }

        this.closeWritableDatabase(db);
    }

    /**
     * 查询最近一次更新的联系人分区时间戳。
     *
     * @return 如果没有数据返回 {@code 0} 值。
     */
    public long queryLastContactZoneTimestamp() {
        long timestamp = 0;

        String sql = "SELECT `timestamp` FROM `contact_zone` WHERE `state`=" + ContactZoneState.Normal.code +
            " ORDER BY `timestamp` DESC LIMIT 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(sql, new String[]{});
        if (cursor.moveToFirst()) {
            timestamp = cursor.getLong(0);
        }
        cursor.close();
        this.closeReadableDatabase(db);

        return timestamp;
    }

    /**
     * 读取指定名称的联系人分区。
     *
     * @param zoneName 分区名。
     * @return
     */
    public ContactZone readContactZone(String zoneName) {
        ContactZone zone = null;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query("contact_zone", new String[] {
                    "id", "display_name", "state", "timestamp", "last", "expiry", "context" },
                "name=?", new String[]{ zoneName },
                null, null, null);

        if (cursor.moveToFirst()) {
            zone = new ContactZone(cursor.getLong(cursor.getColumnIndex("id")), zoneName,
                    cursor.getString(cursor.getColumnIndex("display_name")),
                    cursor.getLong(cursor.getColumnIndex("timestamp")),
                    ContactZoneState.parse(cursor.getInt(cursor.getColumnIndex("state"))));
            // 重置内存管理使用的时间戳
            zone.resetExpiry(cursor.getLong(cursor.getColumnIndex("expiry")),
                    cursor.getLong(cursor.getColumnIndex("last")));

            // 上下文数据
            String contextString = cursor.getString(cursor.getColumnIndex("context"));
            if (null != contextString && contextString.length() > 3) {
                try {
                    zone.setContext(new JSONObject(contextString));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        cursor.close();

        if (null == zone) {
            this.closeReadableDatabase(db);
            return null;
        }

        // 读取参与人
        cursor = db.query("contact_zone_participant", new String[]{
                    "contact_id", "state", "timestamp", "postscript", "context" },
                "contact_zone_id=?", new String[]{ zone.id.toString() },
                null, null, null);
        while (cursor.moveToNext()) {
            ContactZoneParticipant participant = new ContactZoneParticipant(
                    cursor.getLong(cursor.getColumnIndex("contact_id")),
                    cursor.getLong(cursor.getColumnIndex("timestamp")),
                    ContactZoneParticipantState.parse(cursor.getInt(cursor.getColumnIndex("state"))),
                    cursor.getString(cursor.getColumnIndex("postscript")));

            // 上下文数据
            String contextString = cursor.getString(cursor.getColumnIndex("context"));
            if (null != contextString && contextString.length() > 3) {
                try {
                    participant.setContext(new JSONObject(contextString));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            zone.addParticipant(participant);
        }

        cursor.close();
        this.closeReadableDatabase(db);

        return zone;
    }

    /**
     * 写入联系人分区数据。
     *
     * @param zone
     * @return 返回写入的分区是否已存在。
     */
    public boolean writeContactZone(ContactZone zone) {
        boolean exists = false;

        SQLiteDatabase db = this.getWritableDatabase();

        String contextString = (null != zone.getContext()) ? zone.getContext().toString() : "";

        Cursor cursor = db.query("contact_zone", new String[]{ "id" },
                "name=?", new String[]{ zone.name }, null, null, null);

        if (cursor.moveToFirst()) {
            exists = true;
            cursor.close();

            // 已存在，重置参与人数据，删除已存在数据
            if (zone.getParticipants().size() > 0) {
                db.delete("contact_zone_participant", "contact_zone_id=?",
                        new String[]{ zone.id.toString() });
            }

            // 更新数据
            ContentValues values = new ContentValues();
            values.put("display_name", zone.getDisplayName());
            values.put("state", zone.getState().code);
            values.put("timestamp", zone.getTimestamp());
            values.put("last", zone.getLast());
            values.put("expiry", zone.getExpiry());
            values.put("context", contextString);
            // update
            db.update("contact_zone", values, "id=?", new String[]{ zone.id.toString() });
        }
        else {
            exists = false;
            cursor.close();

            // 不存在，插入数据
            ContentValues values = new ContentValues();
            values.put("id", zone.id);
            values.put("name", zone.name);
            values.put("display_name", zone.getDisplayName());
            values.put("state", zone.getState().code);
            values.put("timestamp", zone.getTimestamp());
            values.put("last", zone.getLast());
            values.put("expiry", zone.getExpiry());
            values.put("context", contextString);
            // insert
            db.insert("contact_zone", null, values);
        }

        // 写入参与人数据
        for (ContactZoneParticipant participant : zone.getParticipants()) {
            ContentValues values = new ContentValues();
            values.put("contact_zone_id", zone.id);
            values.put("contact_id", participant.getContactId());
            values.put("state", participant.getState().code);
            values.put("timestamp", participant.getTimestamp());
            values.put("postscript", participant.getPostscript());
            values.put("context", (null != participant.getContext()) ? participant.getContext().toString() : "");
            // insert
            db.insert("contact_zone_participant", null, values);
        }

        this.closeWritableDatabase(db);

        return exists;
    }

    private Group readGroup(Cursor cursor) {
        JSONObject context = null;
        String contextString = cursor.getString(cursor.getColumnIndex("context"));
        if (null != contextString && contextString.length() > 2) {
            try {
                context = new JSONObject(contextString);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Group group = new Group(cursor.getLong(cursor.getColumnIndex("id")),
                cursor.getString(cursor.getColumnIndex("name")),
                cursor.getLong(cursor.getColumnIndex("owner")),
                cursor.getString(cursor.getColumnIndex("tag")),
                cursor.getLong(cursor.getColumnIndex("creation")),
                cursor.getLong(cursor.getColumnIndex("last_active")),
                GroupState.parse(cursor.getInt(cursor.getColumnIndex("state"))));

        long last = cursor.getLong(cursor.getColumnIndex("last"));
        long expiry = cursor.getLong(cursor.getColumnIndex("expiry"));
        group.resetExpiry(expiry, last);

        if (null != context) {
            group.setContext(context);
        }

        return group;
    }

    @Override
    protected void onDatabaseCreate(SQLiteDatabase database) {
        // 联系人表
        database.execSQL("CREATE TABLE IF NOT EXISTS `contact` (`sn` INTEGER PRIMARY KEY AUTOINCREMENT, `id` BIGINT NOT NULL UNIQUE, `name` TEXT, `context` TEXT DEFAULT NULL, `timestamp` BIGINT DEFAULT 0, `last` BIGINT DEFAULT 0, `expiry` BIGINT DEFAULT 0)");

        // 群组表
        database.execSQL("CREATE TABLE IF NOT EXISTS `group` (`sn` INTEGER PRIMARY KEY AUTOINCREMENT, `id` BIGINT NOT NULL UNIQUE, `name` TEXT, `owner` BIGINT, `tag` TEXT, `creation` BIGINT, `last_active` BIGINT, `state` INTEGER, `context` TEXT DEFAULT NULL, `last` BIGINT DEFAULT 0, `expiry` BIGINT DEFAULT 0)");

        // 群成员表
        database.execSQL("CREATE TABLE IF NOT EXISTS `group_member` (`sn` INTEGER PRIMARY KEY AUTOINCREMENT, `group` BIGINT, `contact_id` BIGINT, `timestamp` BIGINT)");

        // 附录表
        database.execSQL("CREATE TABLE IF NOT EXISTS `appendix` (`id` BIGINT PRIMARY KEY, `timestamp` BIGINT, `data` TEXT)");

        // 联系人分区
        database.execSQL("CREATE TABLE IF NOT EXISTS `contact_zone` (`id` BIGINT PRIMARY KEY, `name` TEXT, `display_name` TEXT, `state` INTEGER, `timestamp` BIGINT, `last` BIGINT DEFAULT 0, `expiry` BIGINT DEFAULT 0, `context` TEXT DEFAULT NULL)");

        // 联系人分区参与者
        database.execSQL("CREATE TABLE IF NOT EXISTS `contact_zone_participant` (`sn` INTEGER PRIMARY KEY AUTOINCREMENT, `contact_zone_id` BIGINT, `contact_id` BIGINT, `state` INTEGER, `timestamp` BIGINT, `postscript` TEXT, `context` TEXT DEFAULT NULL)");
    }

    @Override
    protected void onDatabaseUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
    }
}
