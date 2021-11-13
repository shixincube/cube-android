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

import android.util.MutableInt;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import cube.core.Packet;
import cube.filestorage.model.FileAnchor;
import cube.util.LogUtils;

/**
 * 上传队列。
 */
public class UploadQueue {

    private final static String TAG = UploadQueue.class.getSimpleName();

    private FileStorage service;

    private MutableInt fileBlockSize;

    private int concurrentNum = 3;

    private AtomicInteger concurrentCount = new AtomicInteger(0);

    private Queue<FileAnchor> fileAnchorQueue;

    private UploadQueueListener listener;

    public UploadQueue(FileStorage service, MutableInt fileBlockSize) {
        this.service = service;
        this.fileBlockSize = fileBlockSize;
        this.fileAnchorQueue = new ConcurrentLinkedQueue<>();
    }

    public void setListener(UploadQueueListener listener) {
        this.listener = listener;
    }

    /**
     * 将文件锚点入队进行数据上传。
     *
     * @param fileAnchor
     */
    public void enqueue(FileAnchor fileAnchor) {
        // 入队
        this.fileAnchorQueue.offer(fileAnchor);

        if (this.concurrentCount.get() < this.concurrentNum) {
            process();
        }
    }

    protected void process() {
        this.concurrentCount.incrementAndGet();

        this.service.execute(() -> {
            // 将文件依次出队
            while (!fileAnchorQueue.isEmpty()) {
                FileAnchor anchor = fileAnchorQueue.poll();
                if (null == anchor) {
                    break;
                }

                // 回调已启动
                listener.onUploadStarted(anchor);

                try {
                    byte[] data = new byte[fileBlockSize.value];

                    MutableInt length = new MutableInt(0);
                    while ((length.value = anchor.inputStream.read(data)) > 0) {
                        FileFormData formData = new FileFormData(service.getSelf().id,
                                anchor.getFileName(), anchor.getFileSize(), anchor.getLastModified(),
                                anchor.position, length.value);
                        // 设置数据
                        formData.setData(data, 0, length.value);

                        HttpClient client = new HttpClient(service.getServiceURL(), service.getTokenCode(),
                                System.currentTimeMillis());
                        client.requestPost(formData.getInputStream(), formData.getBoundary(), new HttpClient.RequestListener() {
                            @Override
                            public void onConnected(HttpClient client) {
                                // Nothing
                            }
                            @Override
                            public void onProgress(HttpClient client, long totalLength) {
                                // Nothing
                            }

                            @Override
                            public void onFailed(HttpClient client, Exception exception) {
                                listener.onUploadFailed(anchor, FileStorageState.TransmitFailed.code);
                            }

                            @Override
                            public void onCompleted(HttpClient client, int stateCode, Packet packet) {
                                if (stateCode == HttpURLConnection.HTTP_OK) {
                                    anchor.updatePosition(length.value);

                                    // 正在上传数据
                                    listener.onUploading(anchor);

                                    if (anchor.isFinish()) {
                                        // 数据上传完成
                                        try {
                                            JSONObject data = packet.extractServiceData();
                                            anchor.setFileCode(data.getString("fileCode"));

                                            LogUtils.d(TAG, "File anchor : " + anchor.getFileName() + " - " + anchor.getFileCode());
                                        }
                                        catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        listener.onUploadCompleted(anchor);
                                    }
                                }
                                else {
                                    LogUtils.w(TAG, "Error : " + stateCode);
                                    listener.onUploadFailed(anchor, FileStorageState.TransmitFailed.code);
                                }
                            }
                        });
                    }

                    // 关闭流
                    anchor.close();
                } catch (IOException e) {
                    LogUtils.w(TAG, "#process", e);
                    listener.onUploadFailed(anchor, FileStorageState.ReadFileFailed.code);
                    anchor.close();
                } catch (Exception e) {
                    LogUtils.w(TAG, "#process", e);
                    listener.onUploadFailed(anchor, FileStorageState.TransmitFailed.code);
                    anchor.close();
                }
            }

            concurrentCount.decrementAndGet();
        });
    }

    /**
     * 上传事件监听器。
     */
    public interface UploadQueueListener {

        void onUploadStarted(FileAnchor fileAnchor);

        void onUploading(FileAnchor fileAnchor);

        void onUploadCompleted(FileAnchor fileAnchor);

        void onUploadFailed(FileAnchor fileAnchor, int errorCode);
    }
}
