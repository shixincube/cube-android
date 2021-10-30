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

package cube.core;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 存储器数据库连接管理器。
 */
public abstract class AbstractStorage implements Storage {

    private SQLite sqlite;

    private AtomicBoolean opened;

//    private AtomicInteger readDBCounter;
//    private SQLiteDatabase readableDatabase;
//    private AtomicInteger writeDBCounter;
//    private SQLiteDatabase writableDatabase;

    private Queue<SQLiteDatabase> readableDatabaseQueue;

    private Queue<SQLiteDatabase> writableDatabaseQueue;

    public AbstractStorage() {
        this.sqlite = null;
        this.opened = new AtomicBoolean(false);
        this.readableDatabaseQueue = new LinkedList<>();
        this.writableDatabaseQueue = new LinkedList<>();
//        this.readDBCounter = new AtomicInteger(0);
//        this.writeDBCounter = new AtomicInteger(0);
    }

    @Override
    public void open(Context context, String filename, int version) {
        if (null == this.sqlite) {
            this.sqlite = new SQLite(context, filename, version);
            (new Thread() {
                @Override
                public void run() {
                    SQLiteDatabase db = sqlite.getReadableDatabase();
                    synchronized (readableDatabaseQueue) {
                        readableDatabaseQueue.offer(db);
                    }
                }
            }).start();
        }
    }

    @Override
    public void close() {
        if (null != this.sqlite) {
            while (!this.readableDatabaseQueue.isEmpty()) {
                SQLiteDatabase db = this.readableDatabaseQueue.poll();
                db.close();
            }

            while (!this.writableDatabaseQueue.isEmpty()) {
                SQLiteDatabase db = this.writableDatabaseQueue.poll();
                db.close();
            }

            this.sqlite.close();
            this.sqlite = null;
        }

        this.opened.set(false);
    }

    public SQLiteDatabase getWritableDatabase() {
        if (!this.opened.get()) {
            synchronized (this.opened) {
                try {
                    this.opened.wait(10L * 1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        synchronized (this.writableDatabaseQueue) {
            SQLiteDatabase db = this.writableDatabaseQueue.poll();
            if (null == db) {
                db = this.sqlite.getWritableDatabase();
            }
            return db;
        }

//        synchronized (this.writeDBCounter) {
//            if (this.writeDBCounter.incrementAndGet() == 1) {
//                this.writableDatabase = this.sqlite.getWritableDatabase();
//            }
//            return this.writableDatabase;
//        }
    }

    public SQLiteDatabase getReadableDatabase() {
        if (!this.opened.get()) {
            synchronized (this.opened) {
                try {
                    this.opened.wait(10L * 1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        synchronized (this.readableDatabaseQueue) {
            SQLiteDatabase db = this.readableDatabaseQueue.poll();
            if (null == db) {
                db = this.sqlite.getReadableDatabase();
            }
            return db;
        }

//        synchronized (this.readDBCounter) {
//            if (this.readDBCounter.incrementAndGet() == 1) {
//                this.readableDatabase = this.sqlite.getReadableDatabase();
//            }
//            return this.readableDatabase;
//        }
    }

    public void closeWritableDatabase(SQLiteDatabase database) {
        synchronized (this.writableDatabaseQueue) {
            this.writableDatabaseQueue.offer(database);
        }
//        synchronized (this.writeDBCounter) {
//            if (this.writeDBCounter.decrementAndGet() == 0) {
//                this.writableDatabase.close();
//            }
//        }
    }

    public void closeReadableDatabase(SQLiteDatabase database) {
        synchronized (this.readableDatabaseQueue) {
            this.readableDatabaseQueue.offer(database);
        }
//        synchronized (this.readDBCounter) {
//            if (this.readDBCounter.decrementAndGet() == 0) {
//                this.readableDatabase.close();
//            }
//        }
    }

    /**
     * 数据库创建时回调。
     *
     * @param database
     */
    protected abstract void onDatabaseCreate(SQLiteDatabase database);

    /**
     * 数据库版本更新。
     *
     * @param database
     * @param oldVersion
     * @param newVersion
     */
    protected abstract void onDatabaseUpgrade(SQLiteDatabase database, int oldVersion, int newVersion);

    /**
     * SQLite 封装。
     */
    protected class SQLite extends SQLiteOpenHelper {

        public SQLite(Context context, String name, int version) {
            super(context, name, null, version);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            Log.d(AbstractStorage.class.getSimpleName(), "#onCreate : " + getDatabaseName());

            AbstractStorage.this.onDatabaseCreate(database);

            if (!opened.get()) {
                opened.set(true);
                synchronized (opened) {
                    opened.notifyAll();
                }
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
            Log.d(AbstractStorage.class.getSimpleName(), "#onUpgrade : " + getDatabaseName());

            AbstractStorage.this.onDatabaseUpgrade(database, oldVersion, newVersion);

            if (!opened.get()) {
                opened.set(true);
                synchronized (opened) {
                    opened.notifyAll();
                }
            }
        }

        @Override
        public void onOpen(SQLiteDatabase database) {
            super.onOpen(database);

//            Log.d(AbstractStorage.class.getSimpleName(), "#onOpen : " + getDatabaseName());

            if (!opened.get()) {
                opened.set(true);
                synchronized (opened) {
                    opened.notifyAll();
                }
            }
        }
    }
}
