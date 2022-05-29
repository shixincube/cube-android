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

package cube.engine.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 编码串辅助函数库。
 */
public class CodeUtils {

    private CodeUtils() {
    }

    public static String extractProtocol(String codeString) {
        byte[] bytes = codeString.getBytes(StandardCharsets.UTF_8);
        byte[] buf = new byte[bytes.length];
        int length = 0;
        for (int i = 0; i < bytes.length; ++i) {
            if (bytes[i] == ':') {
                break;
            }

            buf[i] = bytes[i];
            ++length;
        }
        return new String(buf, 0, length, StandardCharsets.UTF_8);
    }

    public static String[] extractCubeResourceSegments(String codeString) {
        int index = codeString.indexOf("//");
        if (index < 0) {
            return null;
        }

        String string = codeString.substring(index + 2);
        return string.split("\\.");
    }

    public static boolean isBoxDomain(String codeString) {
        try {
            URL url = new URL(codeString);
            if (url.getHost().equalsIgnoreCase("box.shixincube.com")) {
                if (url.getPath().startsWith("/box")) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }

        return false;
    }

    public static String extractBoxDomain(String codeString) {
        return extractURLLastPath(codeString);
    }

    public static String extractURLLastPath(String codeString) {
        String path = null;
        try {
            URL url = new URL(codeString);
            path = url.getPath().trim();
            int index = path.lastIndexOf("/");
            path = path.substring(index + 1);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return path;
    }
}
