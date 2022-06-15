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

package cube.util;

import android.content.Context;
import android.os.Environment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 文件实用函数库。
 */
public class FileUtils {

    private final static String TAG = "FileUtils";

    private FileUtils() {
    }

    public static File getFilePath(Context context, String pathName) {
        StringBuilder buf = new StringBuilder();

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            buf.append(context.getExternalFilesDir(pathName).getAbsoluteFile());
        }
        else {
            buf.append(context.getFilesDir());
            buf.append(File.separator);
            buf.append(pathName);
        }
        buf.append(File.separator);

        File dir = new File(buf.toString());
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }

        return dir;
    }

    /**
     * 读取 JSON 格式的文件数据。
     *
     * @param file
     * @return
     * @throws JSONException
     */
    public static JSONObject readJSONFile(File file) throws JSONException {
        StringBuilder buf = new StringBuilder();
        BufferedReader reader = null;

        try {
            InputStreamReader isr = new InputStreamReader(new FileInputStream(file),
                    StandardCharsets.UTF_8);
            reader = new BufferedReader(isr);
            String line = null;
            while ((line = reader.readLine()) != null) {
                buf.append(line);
            }
        } catch (FileNotFoundException e) {
            LogUtils.w(TAG, "#readJSONFile", e);
        } catch (IOException e) {
            LogUtils.e(TAG, "#readJSONFile", e);
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }

        if (buf.length() <= 2) {
            return null;
        }

        return new JSONObject(buf.toString());
    }

    public static boolean writeJSONFile(File file, JSONObject json) {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file);
            fos.write(json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // Nothing
                }
            }
        }

        return true;
    }

    /**
     * 复制文件数据。
     *
     * @param source
     * @param target
     * @throws IOException
     */
    public static void copy(File source, File target) throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;

        byte[] buf = new byte[128];
        int length = 0;
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(target);

            while ((length = fis.read(buf)) > 0) {
                fos.write(buf, 0, length);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }

            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
