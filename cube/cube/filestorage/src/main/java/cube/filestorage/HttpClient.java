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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import cube.util.LogUtils;

public class HttpClient {

    private String url;

    public HttpClient(String url) {
        this.url = url;
    }

    public void requestPost(InputStream requestStream, RequestListener listener) {
        OutputStreamWriter writer = null;
        BufferedReader reader = null;

        try {
            URL realURL = new URL(this.url);

            // 建立连接
            HttpURLConnection conn = (HttpURLConnection) realURL.openConnection();
            conn.setConnectTimeout(10 * 1000);

            // 设置通用的请求属性
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Connection", "Keep-Alive");

            // 启用输入、输出流
            conn.setDoInput(true);
            conn.setDoOutput(true);

            // 回调
            listener.onConnected(this);

            // 写入数据
            writer = new OutputStreamWriter(conn.getOutputStream());
            int length = 0;
            byte[] buf = new byte[1024];
            char[] inputBuf = new char[1024];
            while ((length = requestStream.read(buf)) > 0) {
                System.arraycopy(buf, 0, inputBuf, 0, length);
                writer.write(inputBuf, 0, length);
            }
            writer.flush();

            StringBuilder builder = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            int stateCode = conn.getResponseCode();
            conn.disconnect();

            // 回调
            listener.onCompleted(this, stateCode, builder.toString());

        } catch (MalformedURLException e) {
            LogUtils.w(this.getClass().getSimpleName(), e);
        } catch (IOException e) {
            LogUtils.w(this.getClass().getSimpleName(), e);

            listener.onFailed(this, e);
        } finally {
            if (null != writer) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LogUtils.w(this.getClass().getSimpleName(), e);
                }
            }

            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LogUtils.w(this.getClass().getSimpleName(), e);
                }
            }
        }
    }

    /**
     * 请求监听器。
     */
    public interface RequestListener {

        void onConnected(HttpClient client);

        void onFailed(HttpClient client, Exception exception);

        void onCompleted(HttpClient client, int stateCode, String result);
    }
}
