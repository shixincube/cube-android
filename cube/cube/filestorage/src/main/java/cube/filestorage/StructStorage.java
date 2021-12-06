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

import java.util.ArrayList;
import java.util.List;

import cube.core.AbstractStorage;
import cube.filestorage.model.Directory;
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

        Cursor cursor = db.query("file_label", new String[]{ "id" },
                "id=?", new String[]{ fileLabel.id.toString() }, null, null, null);
        if (cursor.moveToFirst()) {
            cursor.close();

            ContentValues values = new ContentValues();
            values.put("timestamp", fileLabel.getTimestamp());
            values.put("owner", fileLabel.getOwnerId());
            values.put("file_code", fileLabel.getFileCode());
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

            if (null != fileLabel.getFilePath()) {
                values.put("file_path", fileLabel.getFilePath());
            }

            // update
            db.update("file_label", values,
                    "id=?", new String[]{ fileLabel.id.toString() });
        }
        else {
            cursor.close();

            ContentValues values = new ContentValues();
            values.put("id", fileLabel.id);
            values.put("timestamp", fileLabel.getTimestamp());
            values.put("owner", fileLabel.getOwnerId());
            values.put("file_code", fileLabel.getFileCode());
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

            if (null != fileLabel.getFilePath()) {
                values.put("file_path", fileLabel.getFilePath());
            }

            // insert
            db.insert("file_label", null, values);
        }

        this.closeWritableDatabase(db);
    }

    /**
     * 指定文件码读取文件标签。
     *
     * @param fileCode
     * @return
     */
    public FileLabel readFileLabel(String fileCode) {
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

    /**
     * 读取目录结构。
     *
     * @param dirId
     * @return
     */
    public Directory readDirectory(Long dirId) {
        Directory directory = null;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query("directory", new String[]{ "*" },
                "id=?", new String[]{ dirId.toString() }, null, null, null);
        if (cursor.moveToFirst()) {
            directory = readDirectory(cursor);
        }
        cursor.close();

        this.closeReadableDatabase(db);
        return directory;
    }

    /**
     * 读取指定目录的子目录。
     *
     * @param parent
     * @return
     */
    public List<Directory> readSubdirectories(Directory parent) {
        List<Directory> result = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        List<Long> dirIdList = new ArrayList<>();
        Cursor cursor = db.query("hierarchy", new String[]{ "dir_id" },
                "parent_id=? AND dir_id<>0", new String[]{ parent.id.toString() },
                null, null, null);
        while (cursor.moveToNext()) {
            Long dirId = cursor.getLong(0);
            dirIdList.add(dirId);
        }
        cursor.close();
        this.closeReadableDatabase(db);

        // 读取目录
        for (Long dirId : dirIdList) {
            Directory dir = this.readDirectory(dirId);
            if (null != dir) {
                result.add(dir);
            }
        }

        return result;
    }

    /**
     * 读取目录下的文件记录。
     *
     * @param parent
     * @return
     */
    public List<FileLabel> readFiles(Directory parent) {
        List<FileLabel> result = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        List<String> codeList = new ArrayList<>();
        Cursor cursor = db.query("hierarchy", new String[]{ "file_code" },
                "parent_id=? AND dir_id=0", new String[]{ parent.id.toString() },
                null, null, null);
        while (cursor.moveToNext()) {
            String fileCode = cursor.getString(0);
            codeList.add(fileCode);
        }
        cursor.close();
        this.closeReadableDatabase(db);

        for (String fileCode : codeList) {
            FileLabel fileLabel = this.readFileLabel(fileCode);
            if (null != fileLabel) {
                result.add(fileLabel);
            }
        }

        return result;
    }

    /**
     * 写入目录结构。
     *
     * @param directory 指定目录。
     */
    public void writeDirectory(Directory directory) {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query("directory", new String[]{ "id" },
                "id=?", new String[]{ directory.id.toString() }, null, null, null);
        if (cursor.moveToFirst()) {
            cursor.close();

            ContentValues values = new ContentValues();
            values.put("name", directory.getName());
            values.put("last_modified", directory.getLastModified());
            values.put("size", directory.getSize());
            values.put("hidden", directory.isHidden() ? 1 : 0);
            values.put("num_dirs", directory.numDirs());
            values.put("num_files", directory.numFiles());
            values.put("parent_id", directory.getParentId().longValue());
            values.put("last", directory.getLast());
            values.put("expiry", directory.getExpiry());
            // update
            db.update("directory", values, "id=?", new String[]{ directory.id.toString() });
        }
        else {
            cursor.close();

            ContentValues values = new ContentValues();
            values.put("id", directory.id.longValue());
            values.put("name", directory.getName());
            values.put("creation", directory.getCreation());
            values.put("last_modified", directory.getLastModified());
            values.put("size", directory.getSize());
            values.put("hidden", directory.isHidden() ? 1 : 0);
            values.put("num_dirs", directory.numDirs());
            values.put("num_files", directory.numFiles());
            values.put("parent_id", directory.getParentId().longValue());
            values.put("last", directory.getLast());
            values.put("expiry", directory.getExpiry());
            // insert
            db.insert("directory", null, values);
        }

        // 存入结构
        cursor = db.query("hierarchy", new String[]{ "sn" },
                "parent_id=? AND dir_id=?", new String[]{
                        directory.getParentId().toString(),
                        directory.id.toString()
                }, null, null, null);
        if (cursor.moveToFirst()) {
            cursor.close();
        }
        else {
            cursor.close();

            ContentValues values = new ContentValues();
            values.put("parent_id", directory.getParentId());
            values.put("dir_id", directory.id);
            values.put("hidden", directory.isHidden() ? 1 : 0);
            // insert
            db.insert("hierarchy", null, values);
        }

        this.closeWritableDatabase(db);
    }

    /**
     * 删除目录。
     *
     * @param directory
     */
    public void deleteDirectory(Directory directory) {
        SQLiteDatabase db = this.getWritableDatabase();

        // delete
        db.delete("directory", "id=?", new String[]{
                directory.id.toString()
        });

        // delete
        db.delete("hierarchy", "dir_id=? OR parent_id=?", new String[]{
                directory.id.toString(),
                directory.id.toString()
        });

        this.closeWritableDatabase(db);
    }

    /**
     * 删除指定目录下的文件。
     *
     * @param directory
     * @param fileLabel
     */
    public void deleteFile(Directory directory, FileLabel fileLabel) {
        SQLiteDatabase db = this.getWritableDatabase();

        // delete
        db.delete("hierarchy", "dir_id=? AND file_code=?", new String[]{
                directory.id.toString(),
                fileLabel.getFileCode()
        });

        this.closeWritableDatabase(db);
    }

    /**
     * 写入目录下的文件标签。
     *
     * @param directory
     * @param fileLabel
     */
    public void writeFileLabel(Directory directory, FileLabel fileLabel) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query("hierarchy", new String[]{ "sn" },
                "parent_id=? AND file_code=?", new String[]{
                        directory.id.toString(),
                        fileLabel.getFileCode() },
                null, null, null);
        if (cursor.moveToFirst()) {
            cursor.close();
        }
        else {
            cursor.close();

            // 插入数据
            ContentValues values = new ContentValues();
            values.put("parent_id", directory.id);
            values.put("file_code", fileLabel.getFileCode());
            // insert
            db.insert("hierarchy", null, values);
        }
        this.closeWritableDatabase(db);

        // 写入文件标签
        this.writeFileLabel(fileLabel);
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

    private Directory readDirectory(Cursor cursor) {
        return new Directory(cursor.getLong(cursor.getColumnIndex("id")),
                cursor.getString(cursor.getColumnIndex("name")),
                cursor.getLong(cursor.getColumnIndex("creation")),
                cursor.getLong(cursor.getColumnIndex("last_modified")),
                cursor.getLong(cursor.getColumnIndex("size")),
                cursor.getInt(cursor.getColumnIndex("hidden")) == 1,
                cursor.getInt(cursor.getColumnIndex("num_dirs")),
                cursor.getInt(cursor.getColumnIndex("num_files")),
                cursor.getLong(cursor.getColumnIndex("parent_id")),
                cursor.getLong(cursor.getColumnIndex("last")),
                cursor.getLong(cursor.getColumnIndex("expiry")));
    }

    @Override
    protected void onDatabaseCreate(SQLiteDatabase database) {
        // 本地文件记录
        database.execSQL("CREATE TABLE IF NOT EXISTS `file_label` (`id` BIGINT PRIMARY KEY, `timestamp` BIGINT, `owner` BIGINT, `file_code` TEXT, `file_path` TEXT DEFAULT NULL, `file_name` TEXT, `file_size` BIGINT, `last_modified` BIGINT, `completed_time` BIGINT, `expiry_time` BIGINT, `file_type` TEXT, `md5` TEXT, `sha1` TEXT, `file_url` TEXT, `file_secure_url` TEXT)");

        // 目录基本信息
        database.execSQL("CREATE TABLE IF NOT EXISTS `directory` (`id` BIGINT PRIMARY KEY, `name` TEXT, `creation` BIGINT, `last_modified` BIGINT, `size` BIGINT, `hidden` INTEGER, `num_dirs` INTEGER, `num_files` INTEGER, `parent_id` BIGINT DEFAULT 0, `last` BIGINT DEFAULT 0, `expiry` BIGINT DEFAULT 0)");

        // 层级结构
        database.execSQL("CREATE TABLE IF NOT EXISTS `hierarchy` (`sn` INTEGER PRIMARY KEY AUTOINCREMENT, `parent_id` BIGINT, `dir_id` BIGINT DEFAULT 0, `file_code` TEXT DEFAULT NULL, `hidden` INTEGER DEFAULT 0)");
    }

    @Override
    protected void onDatabaseUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {

    }
}
