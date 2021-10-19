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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import cube.core.Storage;

/**
 * 联系人模块存储器。
 */
public class ContactStorage implements Storage {

    private final static int VERSION = 1;

    private SQLite sqLite;

    private Long contactId;

    private String domain;

    public ContactStorage() {
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
        if (null == this.sqLite) {
            this.contactId = contactId;
            this.domain = domain;
            this.sqLite = new SQLite(context);
        }

        return true;
    }

    @Override
    public void close() {
        if (null != this.sqLite) {
            this.sqLite.close();
            this.sqLite = null;
        }
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
