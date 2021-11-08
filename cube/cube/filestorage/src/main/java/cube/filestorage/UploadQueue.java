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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import cube.filestorage.model.FileAnchor;
import cube.util.LogUtils;

/**
 * 上传队列。
 */
public class UploadQueue {

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

    public void enqueue(FileAnchor fileAnchor) {
        // 入队
        this.fileAnchorQueue.offer(fileAnchor);

        if (this.concurrentCount.get() < this.concurrentNum) {
            process();
        }
    }

    protected void process() {
        this.concurrentCount.incrementAndGet();

        this.service.execute(new Runnable() {
            @Override
            public void run() {
                // 将文件依次出队
                while (!fileAnchorQueue.isEmpty()) {
                    FileAnchor anchor = fileAnchorQueue.poll();
                    if (null == anchor) {
                        break;
                    }

                    try {
                        byte[] data = new byte[fileBlockSize.value];

                        MutableInt length = new MutableInt(0);
                        while ((length.value = anchor.inputStream.read(data)) > 0) {
                            FileFormData formData = new FileFormData(service.getSelf().id,
                                    anchor.filename, anchor.fileSize, anchor.lastModified,
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
                                public void onFailed(HttpClient client, Exception exception) {
                                    listener.onUploadFailed(anchor);
                                }

                                @Override
                                public void onCompleted(HttpClient client, int stateCode, String result) {
                                    if (stateCode == 200) {
                                        anchor.updatePosition(length.value);

                                        try {
                                            JSONObject data = new JSONObject(result);
                                            System.out.println("XJW : " + data.toString());
                                        }
                                        catch (JSONException e) {
                                            // Nothing
                                        }

                                        if (anchor.isFinish()) {
                                            listener.onUploadCompleted(anchor);
                                        }
                                        else {
                                            listener.onUploading(anchor);
                                        }
                                    }
                                    else {
                                        LogUtils.w(UploadQueue.class.getSimpleName(), "Error : " + stateCode + "\r\n" + result);
                                        listener.onUploadFailed(anchor);
                                    }
                                }
                            });
                        }

                        // 关闭流
                        anchor.close();
                    } catch (IOException e) {
                        LogUtils.w(UploadQueue.class.getSimpleName(), e);
                        listener.onUploadFailed(anchor);
                        anchor.close();
                    } catch (Exception e) {
                        LogUtils.w(UploadQueue.class.getSimpleName(), e);
                        listener.onUploadFailed(anchor);
                        anchor.close();
                    }
                }

                concurrentCount.decrementAndGet();
            }
        });
    }

    /**
     * 上传事件监听器。
     */
    public interface UploadQueueListener {

        void onUploading(FileAnchor fileAnchor);

        void onUploadCompleted(FileAnchor fileAnchor);

        void onUploadFailed(FileAnchor fileAnchor);
    }
}
