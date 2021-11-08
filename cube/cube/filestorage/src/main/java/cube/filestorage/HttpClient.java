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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import cube.util.LogUtils;

/**
 * HTTP 客户端封装。
 */
public class HttpClient {

    private String url;

    private String token;

    private String sn;

    public HttpClient(String url, String token, Long sn) {
        this.url = url;
        this.url += "?token=" + token;
        this.url += "&sn=" + sn;
    }

    public void requestPost(InputStream requestStream, String boundary, RequestListener listener) {
        DataOutputStream writer = null;
        BufferedReader reader = null;

        try {
            URL realURL = new URL(this.url);

            // 建立连接
            HttpURLConnection conn = (HttpURLConnection) realURL.openConnection();
            conn.setConnectTimeout(5 * 1000);
            conn.setReadTimeout(10 * 1000);
            conn.setRequestMethod("POST");

            LogUtils.d(HttpClient.class.getSimpleName(), "Request [POST] : " + this.url);

            // 设置通用的请求属性
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Cache-Control", "no-cache");
//            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            conn.setRequestProperty("Charset", "UTF-8");

            // 启用输入、输出流
            conn.setDoInput(true);
            conn.setDoOutput(true);

            // 回调
            listener.onConnected(this);

            // 写入数据
            writer = new DataOutputStream(conn.getOutputStream());
            int length = 0;
            byte[] buf = new byte[1024];
            while ((length = requestStream.read(buf)) > 0) {
                writer.write(buf, 0, length);
            }
            writer.flush();

            StringBuilder builder = new StringBuilder();

            int stateCode = conn.getResponseCode();
            if (HttpURLConnection.HTTP_OK == stateCode ||
                HttpURLConnection.HTTP_CREATED == stateCode ||
                HttpURLConnection.HTTP_ACCEPTED == stateCode) {

                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            }
            else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            }

            conn.disconnect();

            // 回调
            listener.onCompleted(this, stateCode, builder.toString());

        } catch (MalformedURLException e) {
            LogUtils.w(this.getClass().getSimpleName(), e);
        } catch (IOException e) {
            LogUtils.w(this.getClass().getSimpleName(), e);
            // 回调
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
