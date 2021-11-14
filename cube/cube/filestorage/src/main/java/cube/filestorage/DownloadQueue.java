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

import androidx.annotation.Nullable;

import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import cube.core.Packet;
import cube.filestorage.model.FileAnchor;
import cube.util.LogUtils;

/**
 * 下载队列。
 */
public class DownloadQueue {

    private final static String TAG = DownloadQueue.class.getSimpleName();

    private FileStorage service;

    private MutableInt fileBlockSize;

    private int concurrentNum = 3;

    private AtomicInteger concurrentCount = new AtomicInteger(0);

    private Queue<FileAnchor> fileAnchorQueue;

    private List<FileAnchor> processingList;

    private DownloadQueueListener listener;

    public DownloadQueue(FileStorage service, MutableInt fileBlockSize) {
        this.service = service;
        this.fileBlockSize = fileBlockSize;
        this.fileAnchorQueue = new ConcurrentLinkedQueue<>();
        this.processingList = new Vector<>();
    }

    public void setListener(DownloadQueueListener listener) {
        this.listener = listener;
    }

    public boolean isProcessing(String fileCode) {
        for (FileAnchor anchor : processingList) {
            if (anchor.getFileCode().equals(fileCode)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断指定的文件锚是否正在被处理。
     *
     * @param fileAnchor
     * @return
     */
    public boolean isProcessing(FileAnchor fileAnchor) {
        return this.processingList.contains(fileAnchor);
    }

    public void enqueue(FileAnchor fileAnchor) {
        if (this.processingList.contains(fileAnchor)) {
            return;
        }

        // 入队
        this.fileAnchorQueue.offer(fileAnchor);
        this.processingList.add(fileAnchor);

        if (this.concurrentCount.get() < this.concurrentNum) {
            process();
        }
    }

    private void process() {
        this.concurrentCount.incrementAndGet();

        this.service.execute(() -> {
            // 将文件依次出队
            while (!fileAnchorQueue.isEmpty()) {
                FileAnchor anchor = fileAnchorQueue.poll();
                if (null == anchor) {
                    break;
                }

                // 回调
                listener.onDownloadStarted(anchor);

                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(anchor.getFile());

                    HttpClient httpClient = new HttpClient(anchor.getFileURL());
                    httpClient.requestGet(fos, new HttpClient.RequestListener() {
                        @Override
                        public void onConnected(HttpClient client) {
                            // Nothing
                        }

                        @Override
                        public void onProgress(HttpClient client, long totalLength) {
                            listener.onDownloading(anchor.resetPosition(totalLength));
                        }

                        @Override
                        public void onFailed(HttpClient client, Exception exception) {
                            processingList.remove(anchor);
                            listener.onDownloadFailed(anchor, FileStorageState.TransmitFailed.code);
                        }

                        @Override
                        public void onCompleted(HttpClient client, int stateCode, @Nullable Packet packet) {
                            if (stateCode == HttpURLConnection.HTTP_OK) {
                                processingList.remove(anchor);
                                // 成功
                                listener.onDownloadCompleted(anchor);
                            }
                            else {
                                processingList.remove(anchor);
                                // 失败
                                listener.onDownloadFailed(anchor, stateCode);
                            }
                        }
                    });
                } catch (Exception e) {
                    LogUtils.w(TAG, "#process", e);
                    processingList.remove(anchor);
                    listener.onDownloadFailed(anchor, FileStorageState.TransmitFailed.code);
                    anchor.close();
                }
            }

            concurrentCount.decrementAndGet();
        });
    }

    /**
     * 下载事件监听器。
     */
    public interface DownloadQueueListener {

        void onDownloadStarted(FileAnchor fileAnchor);

        void onDownloading(FileAnchor fileAnchor);

        void onDownloadCompleted(FileAnchor fileAnchor);

        void onDownloadFailed(FileAnchor fileAnchor, int errorCode);
    }
}
