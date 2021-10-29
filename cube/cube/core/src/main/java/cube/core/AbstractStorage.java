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

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 存储器数据库连接管理器。
 */
public abstract class AbstractStorage implements Storage {

    protected SQLiteOpenHelper sqliteHelper;

    private AtomicInteger readDBCounter;

    private SQLiteDatabase readableDatabase;

    private AtomicInteger writeDBCounter;

    private SQLiteDatabase writableDatabase;

    public AbstractStorage() {
        this.readDBCounter = new AtomicInteger(0);
        this.writeDBCounter = new AtomicInteger(0);
    }

    public AbstractStorage(SQLiteOpenHelper sqliteHelper) {
        this.sqliteHelper = sqliteHelper;
        this.readDBCounter = new AtomicInteger(0);
        this.writeDBCounter = new AtomicInteger(0);
    }

    public void setSqliteHelper(SQLiteOpenHelper sqliteHelper) {
        this.sqliteHelper = sqliteHelper;
    }

    public SQLiteDatabase getWritableDatabase() {
        synchronized (this.writeDBCounter) {
            if (this.writeDBCounter.incrementAndGet() == 1) {
                this.writableDatabase = this.sqliteHelper.getWritableDatabase();
            }
            return this.writableDatabase;
        }
    }

    public SQLiteDatabase getReadableDatabase() {
        synchronized (this.readDBCounter) {
            if (this.readDBCounter.incrementAndGet() == 1) {
                this.readableDatabase = this.sqliteHelper.getReadableDatabase();
            }
            return this.readableDatabase;
        }
    }

    public void closeWritableDatabase() {
        synchronized (this.writeDBCounter) {
            if (this.writeDBCounter.decrementAndGet() == 0) {
                this.writableDatabase.close();
            }
        }
    }

    public void closeReadableDatabase() {
        synchronized (this.readDBCounter) {
            if (this.readDBCounter.decrementAndGet() == 0) {
                this.readableDatabase.close();
            }
        }
    }
}
