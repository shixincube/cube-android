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

import java.net.MalformedURLException;
import java.net.URL;

import cube.core.Kernel;

/**
 * URL 实用函数库。
 */
public final class URLUtils {

    private URLUtils() {
    }

    /**
     * 修正文件 URL 。
     *
     * @param fileURL
     * @return
     */
    public static String correctFileURL(String fileURL) {
        if (null == fileURL) {
            return null;
        }

        String currentHost = Kernel.getDefault().getConfig().address;
        if (!IPUtils.isIPv4(currentHost)) {
            return fileURL;
        }

        String newFileURL = fileURL;

        try {
            URL url = new URL(fileURL);
            String host = url.getHost();

            if (!host.equals(currentHost)) {
                // IP 不一致，使用当前 IP
                int start = fileURL.indexOf("://");
                int end = fileURL.substring(start + 3).indexOf(":") + start + 3;

                String head = fileURL.substring(0, start + 3);
                String tail = fileURL.substring(end, fileURL.length());

                StringBuilder result = new StringBuilder(head);
                result.append(currentHost);
                result.append(tail);
                newFileURL = result.toString();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return newFileURL;
    }
}
