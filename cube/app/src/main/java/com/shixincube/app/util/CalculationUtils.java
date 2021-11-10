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

package com.shixincube.app.util;

import android.annotation.SuppressLint;

/**
 * 单位计算实用函数库。
 */
public final class CalculationUtils {

    private final static long SIZE_KB = 1024;
    private final static long SIZE_MB = SIZE_KB * 1024;
    private final static long SIZE_GB = SIZE_MB * 1024;
    private final static long SIZE_TB = SIZE_GB * 1024;

    private CalculationUtils() {
    }

    /**
     * 格式化输出数据大小。
     * 例如计算由字节描述的文件大小，转为可视化的字符串形式。
     *
     * @param dataSizeInBytes
     * @return
     */
    @SuppressLint("DefaultLocale")
    public static String formatByteDataSize(long dataSizeInBytes) {
        if (dataSizeInBytes < SIZE_KB) {
            return Long.toString(dataSizeInBytes) + " B";
        }
        else if (dataSizeInBytes < SIZE_MB) {
            return String.format("%.2f KB", (double) dataSizeInBytes / (double) SIZE_KB);
        }
        else if (dataSizeInBytes < SIZE_GB) {
            return String.format("%.2f MB", (double) dataSizeInBytes / (double) SIZE_MB);
        }
        else if (dataSizeInBytes < SIZE_TB) {
            return String.format("%.2f GB", (double) dataSizeInBytes / (double) SIZE_GB);
        }
        else {
            return String.format("%.2f TB", (double) dataSizeInBytes / (double) SIZE_TB);
        }
    }
}
