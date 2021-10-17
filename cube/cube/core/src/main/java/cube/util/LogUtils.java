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

package cube.util;

import android.text.TextUtils;
import android.util.Log;

import java.util.List;

/**
 * 日志实用函数库。
 */
public final class LogUtils {

    /**
     * 日志输出时的 TAG 。
     */
    private final static String TAG = "Cube";

    /**
     * 日志输出级别 NONE 。
     */
    public static final int LEVEL_OFF = 0;
    /**
     * 日志输出级别 ALL 。
     */
    public static final int LEVEL_ALL = 7;

    /**
     * 日志输出级别 VERBOSE 。
     */
    public static final int LEVEL_VERBOSE = 1;
    /**
     * 日志输出级别 DEBUG 。
     */
    public static final int LEVEL_DEBUG = 2;
    /**
     * 日志输出级别 INFO 。
     */
    public static final int LEVEL_INFO = 3;
    /**
     * 日志输出级别 WARN 。
     */
    public static final int LEVEL_WARN = 4;
    /**
     * 日志输出级别 ERROR 。
     */
    public static final int LEVEL_ERROR = 5;
    /**
     * 日志输出级别 SYSTEM ，自定义定义的一个级别。
     */
    public static final int LEVEL_SYSTEM = 6;

    /**
     * 全局日志等级。
     */
    private static int sLogLevel = LEVEL_ALL;

    /**
     * 用于记时的变量。
     */
    private static long sTimestamp = 0;
    /**
     * 写文件的锁对象
     */
    private static final Object sLogLock = new Object();

    private LogUtils() {
    }

    /**
     * 以级别为 VERBOSE 的形式输出日志。
     * @param log 日志内容。
     */
    public static void v(String log) {
        if (sLogLevel >= LEVEL_VERBOSE) {
            Log.v(TAG, log);
        }
    }

    /**
     * 以级别为 DEBUG 的形式输出日志。
     * @param log 日志内容。
     */
    public static void d(String log) {
        if (sLogLevel >= LEVEL_DEBUG) {
            Log.d(TAG, log);
        }
    }

    /**
     * 以级别为 INFO 的形式输出日志。
     * @param log 日志内容。
     */
    public static void i(String log) {
        if (sLogLevel >= LEVEL_INFO) {
            Log.i(TAG, log);
        }
    }

    /**
     * 以级别为 WARN 的形式输出日志。
     * @param log 日志内容。
     */
    public static void w(String log) {
        if (sLogLevel >= LEVEL_WARN) {
            Log.w(TAG, log);
        }
    }

    /**
     * 以级别为 WARN 的形式输出异常信息。
     * @param throwable 异常描述。
     */
    public static void w(Throwable throwable) {
        if (sLogLevel >= LEVEL_WARN) {
            Log.w(TAG, "", throwable);
        }
    }

    /**
     * 以级别为 WARN 的形式输出日志信息和异常信息。
     * @param log 日志内容。
     * @param throwable 异常描述。
     */
    public static void w(String log, Throwable throwable) {
        if (sLogLevel >= LEVEL_WARN && null != log) {
            Log.w(TAG, log, throwable);
        }
    }

    /**
     * 以级别为 ERROR 的形式输出日志。
     * @param log 日志内容。
     */
    public static void e(String log) {
        if (sLogLevel >= LEVEL_ERROR) {
            Log.e(TAG, log);
        }
    }

    /**
     * 以级别为 ERROR 的形式输出异常信息。
     * @param throwable 异常描述。
     */
    public static void e(Throwable throwable) {
        if (sLogLevel >= LEVEL_ERROR) {
            Log.e(TAG, "", throwable);
        }
    }

    /**
     * 以级别为 ERROR 的形式输出日志信息和异常信息。
     * @param log 日志内容。
     * @param throwable 异常描述。
     */
    public static void e(String log, Throwable throwable) {
        if (sLogLevel >= LEVEL_ERROR && null != throwable) {
            Log.e(TAG, log, throwable);
        }
    }

    /**
     * 以级别为 ERROR 的形式打印日志。
     * @param log 日志内容。
     */
    public static void pe(String log) {
        if (sLogLevel >= LEVEL_ERROR) {
            System.out.println("ERROR: " + log);
        }
    }

    /**
     * 以级别为 ERROR 的形式打印异常信息。
     * @param throwable 日志内容。
     */
    public static void pe(Throwable throwable) {
        if (sLogLevel >= LEVEL_ERROR) {
            System.out.println("ERROR: " + throwable.getLocalizedMessage());
        }
    }

    /**
     * 以级别为 INFO 的形式打印日志。
     * @param log 日志内容。
     */
    public static void p(String log) {
        if (sLogLevel >= LEVEL_INFO) {
            System.out.println(log);
        }
    }

    /**
     * 以级别为 VERBOSE 的形式输出日志。
     * @param tag 日志标签。
     * @param log 日志内容。
     */
    public static void v(String tag, String log) {
        if (sLogLevel >= LEVEL_VERBOSE) {
            Log.v(tag, log);
        }
    }

    /**
     * 以级别为 DEBUG 的形式输出日志。
     * @param tag 日志标签。
     * @param log 日志内容。
     */
    public static void d(String tag, String log) {
        if (sLogLevel >= LEVEL_DEBUG) {
            Log.d(tag, log);
        }
    }

    /**
     * 以级别为 INFO 的形式输出日志。
     * @param tag 日志标签。
     * @param log 日志内容。
     */
    public static void i(String tag, String log) {
        if (sLogLevel >= LEVEL_INFO) {
            Log.i(tag, log);
        }
    }

    /**
     * 以级别为 WARN 的形式输出日志。
     * @param tag 日志标签。
     * @param log 日志内容。
     */
    public static void w(String tag, String log) {
        if (sLogLevel >= LEVEL_WARN) {
            Log.w(tag, log);
        }
    }

    /**
     * 以级别为 ERROR 的形式输出日志。
     * @param tag 日志标签。
     * @param log 日志内容。
     */
    public static void e(String tag, String log) {
        if (sLogLevel >= LEVEL_ERROR) {
            Log.e(tag, log);
        }
    }

    /**
     * 将日志存储到文件。
     *
     * @param log 需要存储的日志。
     * @param path 存储路径。
     */
    public static void log2File(String log, String path) {
        log2File(log, path, true);
    }

    /**
     * 将日志存储到文件。
     *
     * @param log 需要存储的日志。
     * @param path 存储路径。
     * @param append 是否追加内容。
     */
    public static void log2File(String log, String path, boolean append) {
        synchronized (sLogLock) {
//            FileUtils.writeFile(log + "\r\n", path, append);
        }
    }

    /**
     * 以 ERROR 带时间输出日志。
     *
     * @param log 日志内容。
     */
    public static void logWithTime(String log) {
        sTimestamp = System.currentTimeMillis();
        if (!TextUtils.isEmpty(log)) {
            e("[Start：" + sTimestamp + "]" + log);
        }
    }

    /**
     * 以 ERROR 级别输出耗时日志。
     *
     * @param log 日志内容。
     */
    public static void elapsed(String log) {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - sTimestamp;
        sTimestamp = currentTime;
        e("[Elapsed：" + elapsedTime + "]" + log);
    }

    public static <T> void printList(List<T> list) {
        if (list == null || list.size() < 1) {
            return;
        }
        int size = list.size();
        i("---begin---");
        for (int i = 0; i < size; i++) {
            i(i + ":" + list.get(i).toString());
        }
        i("---end---");
    }

    public static <T> void printArray(T[] array) {
        if (array == null || array.length < 1) {
            return;
        }
        int length = array.length;
        i("---begin---");
        for (int i = 0; i < length; i++) {
            i(i + ":" + array[i].toString());
        }
        i("---end---");
    }

    public static void logParameters(String url, String[]... parameter) {
        LogUtils.e("url = " + url);
        for (String[] p : parameter) {
            LogUtils.e(p[0] + " = " + p[1]);
        }
    }
}
