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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import cube.util.LogUtils;
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
        String path = UIUtils.getContext().getExternalFilesDir(dir).getAbsolutePath();
        StringBuilder buf = new StringBuilder();
        buf.append(path);
        if (!path.endsWith(File.separator)) {
            buf.append(File.separator);
        }
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

    /**
     * 下载文件。
     *
     * @param url
     * @param outputFile
     * @throws
     */
    public static void downloadFile(URL url, File outputFile,
                                    DownloadListener listener) throws IOException {
        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        HttpURLConnection conn = null;

        boolean interrupt = false;

        try {
            fos = new FileOutputStream(outputFile);

            // 建立连接
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5 * 1000);
            conn.setReadTimeout(10 * 1000);
            conn.setRequestMethod("GET");

            LogUtils.d("FileUtils", "HTTP [GET]: " + url.toString());

            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setRequestProperty("Charset", "UTF-8");

            // 连接
            conn.connect();

            long fileSize = 0;
            long processedSize = 0;

            int stateCode = conn.getResponseCode();
            if (HttpURLConnection.HTTP_OK == stateCode ||
                    HttpURLConnection.HTTP_CREATED == stateCode ||
                    HttpURLConnection.HTTP_ACCEPTED == stateCode) {
                bis = new BufferedInputStream(conn.getInputStream());
                fileSize = conn.getContentLength();

                // 回调
                listener.onStarted(url, outputFile, fileSize);

                byte[] buf = new byte[1024];
                int length = 0;
                while ((length = bis.read(buf)) > 0) {
                    fos.write(buf, 0, length);

                    processedSize += length;
                    // 回调
                    if (!listener.onDownloading(url, outputFile, fileSize, processedSize)) {
                        // 返回 false 表示结束
                        interrupt = true;
                        LogUtils.i("FileUtils", "Interrupt download: " + url.toString());
                        break;
                    }
                }
            }
        } catch (MalformedURLException | ProtocolException e) {
            LogUtils.w("FileUtils", e);
        } catch (IOException e) {
            LogUtils.w("FileUtils", e);
            throw e;
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // Nothing
                }
            }

            if (null != bis) {
                try {
                    bis.close();
                } catch (IOException e) {
                    // Nothing
                }
            }

            // 关闭连接
            if (null != conn) {
                conn.disconnect();
            }

            if (interrupt) {
                outputFile.delete();
            }

            // 回调
            listener.onCompleted(url, outputFile);
        }
    }

    public interface DownloadListener {

        void onStarted(URL url, File file, long fileSize);

        boolean onDownloading(URL url, File file, long totalSize, long processedSize);

        void onCompleted(URL url, File file);
    }
}
