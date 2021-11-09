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

package cube.filestorage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import cube.core.AbstractStorage;
import cube.filestorage.model.FileLabel;

/**
 * 文件存储服务的数据存储。
 */
public class StructStorage extends AbstractStorage {

    private final static int VERSION = 1;

    private String domain;

    public StructStorage() {
        super();
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
        super.open(context, "CubeFileStorage_" + domain + "_" + contactId + ".db", VERSION);
        this.domain = domain;
    }

    /**
     * 写入文件标签。
     *
     * @param fileLabel
     */
    public void writeFileLabel(FileLabel fileLabel) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("id", fileLabel.id);
        values.put("timestamp", fileLabel.getTimestamp());
        values.put("owner", fileLabel.getOwnerId());
        values.put("file_code", fileLabel.getFileCode());
        values.put("file_path", fileLabel.getFilePath());
        values.put("file_name", fileLabel.getFileName());
        values.put("file_size", fileLabel.getFileSize());
        values.put("last_modified", fileLabel.getLastModified());
        values.put("completed_time", fileLabel.getCompletedTime());
        values.put("expiry_time", fileLabel.getExpiryTime());
        values.put("file_type", fileLabel.getFileType());
        values.put("md5", fileLabel.getMd5Code());
        values.put("sha1", fileLabel.getSha1Code());
        values.put("file_url", fileLabel.getURL());
        values.put("file_secure_url", fileLabel.getSecureURL());

        db.insert("file_label", null, values);

        this.closeWritableDatabase(db);
    }

    /**
     * 指定文件码读取文件标签。
     *
     * @param fileCode
     * @return
     */
    public FileLabel readFileLabelByFileCode(String fileCode) {
        FileLabel fileLabel = null;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM `file_label` WHERE `file_code`=?",
                new String[]{ fileCode });
        if (cursor.moveToFirst()) {
            fileLabel = readFileLabel(cursor);
        }

        cursor.close();
        this.closeReadableDatabase(db);

        return fileLabel;
    }

    private FileLabel readFileLabel(Cursor cursor) {
        return new FileLabel(cursor.getLong(cursor.getColumnIndex("id")),
                cursor.getLong(cursor.getColumnIndex("timestamp")),
                cursor.getLong(cursor.getColumnIndex("owner")),
                cursor.getString(cursor.getColumnIndex("file_code")),
                cursor.getString(cursor.getColumnIndex("file_path")),
                cursor.getString(cursor.getColumnIndex("file_name")),
                cursor.getLong(cursor.getColumnIndex("file_size")),
                cursor.getLong(cursor.getColumnIndex("last_modified")),
                cursor.getLong(cursor.getColumnIndex("completed_time")),
                cursor.getLong(cursor.getColumnIndex("expiry_time")),
                cursor.getString(cursor.getColumnIndex("file_type")),
                cursor.getString(cursor.getColumnIndex("md5")),
                cursor.getString(cursor.getColumnIndex("sha1")),
                cursor.getString(cursor.getColumnIndex("file_url")),
                cursor.getString(cursor.getColumnIndex("file_secure_url")));
    }

    @Override
    protected void onDatabaseCreate(SQLiteDatabase database) {
        // 本地文件记录
        database.execSQL("CREATE TABLE IF NOT EXISTS `file_label` (`id` BIGINT PRIMARY KEY, `timestamp` BIGINT, `owner` BIGINT, `file_code` TEXT, `file_path` TEXT, `file_name` TEXT, `file_size` BIGINT, `last_modified` BIGINT, `completed_time` BIGINT, `expiry_time` BIGINT, `file_type` TEXT, `md5` TEXT, `sha1` TEXT, `file_url` TEXT, `file_secure_url` TEXT)");
    }

    @Override
    protected void onDatabaseUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {

    }
}
