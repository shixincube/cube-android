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

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.ResponseBody;

/**
 * 文件实用函数。
 */
public final class FileUtils {

    private FileUtils() {
    }

    /**
     * 获取指定名称的存储目录。
     *
     * @param name
     * @return
     */
    public static String getDir(String name) {
        StringBuilder buf = new StringBuilder();
        if (isSDCardAvailable()) {
            buf.append(getExternalStoragePath(name));
        }
        else {
            buf.append(getCachePath());
            buf.append(name);
            buf.append(File.separator);
        }

        String path = buf.toString();
        if (createDirs(path)) {
            return path;
        }
        else {
            return null;
        }
    }

    /**
     * 判断 SD 卡是否挂载。
     */
    public static boolean isSDCardAvailable() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * 外部存储路径。
     *
     * @return
     */
    public static String getExternalStoragePath(String dir) {
        StringBuilder buf = new StringBuilder();
        buf.append(UIUtils.getContext().getExternalFilesDir(dir).getAbsolutePath());
        buf.append(File.separator);
        return buf.toString();
    }

    /**
     * 缓存路径。
     *
     * @return
     */
    public static String getCachePath() {
        File dir = UIUtils.getContext().getCacheDir();
        if (null == dir) {
            return null;
        }
        else {
            return dir.getAbsolutePath() + File.separator;
        }
    }

    /**
     * 创建文件夹。
     *
     * @param dirPath
     * @return
     */
    public static boolean createDirs(String dirPath) {
        File file = new File(dirPath);
        if (!file.exists() || !file.isDirectory()) {
            return file.mkdirs();
        }
        return true;
    }

    /**
     * 复制文件数据。
     *
     * @param source
     * @param target
     * @return
     */
    public static boolean copy(File source, File target) {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(source);
            out = new FileOutputStream(target);
            int c = -1;
            while ((c = in.read()) != -1) {
                out.write(c);
            }
            out.flush();
        } catch (IOException e) {
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Nothing
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // Nothing
                }
            }
        }

        return true;
    }

    /**
     * 保存文件。
     *
     * @param outputFile
     * @param body
     * @return
     */
    public static boolean save(File outputFile, ResponseBody body) {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = body.byteStream();
            out = new FileOutputStream(outputFile);
            int c = -1;
            while ((c = in.read()) != -1) {
                out.write(c);
            }
            out.flush();
        } catch (IOException e) {
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }
}
