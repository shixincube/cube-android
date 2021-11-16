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

package cube.messaging;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

import cube.core.ModuleError;
import cube.fileprocessor.model.FileThumbnail;
import cube.filestorage.handler.StableDownloadFileHandler;
import cube.filestorage.model.FileAnchor;
import cube.filestorage.model.FileLabel;
import cube.messaging.model.Message;
import cube.util.LogUtils;

/**
 * 缩略图下载管理。
 */
public final class ThumbnailDownloadManager extends StableDownloadFileHandler {

    private final static ThumbnailDownloadManager instance = new ThumbnailDownloadManager();

    private ExecutorService executor;

    private List<ThumbnailDownloadTask> taskList;
    private List<String> fileCodes;

    private ThumbnailDownloadManager() {
        this.taskList = new ArrayList<>();
        this.fileCodes = new ArrayList<>();
    }

    public final static ThumbnailDownloadManager getInstance() {
        return ThumbnailDownloadManager.instance;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public void schedule(ThumbnailDownloadTask task) {
        String fileCode = task.getFileCode();

        synchronized (this.fileCodes) {
            this.taskList.add(task);

            if (!this.fileCodes.contains(fileCode)) {
                this.fileCodes.add(fileCode);

                this.executor.execute(() -> {
                    task.fileStorageService.downloadFile(task.thumbnail.getFileLabel(), ThumbnailDownloadManager.instance);
                });
            }
        }
    }

    @Override
    public void handleStarted(FileAnchor anchor) {
        // Nothing
    }

    @Override
    public void handleProcessing(FileAnchor anchor) {
        // Nothing
    }

    @Override
    public void handleSuccess(FileAnchor anchor, FileLabel fileLabel) {
        String fileCode = fileLabel.getFileCode();
        HashMap<Long, Message> messageMap = new HashMap<>();
        MessagingStorage storage = null;

        synchronized (this.fileCodes) {
            Iterator<ThumbnailDownloadTask> iter = this.taskList.iterator();
            while (iter.hasNext()) {
                ThumbnailDownloadTask task = iter.next();
                if (task.getFileCode().equals(fileCode)) {
                    // 移除
                    iter.remove();

                    JSONObject context = task.parentFileLabel.getContext();
                    FileThumbnail thumbnail = null;
                    try {
                        thumbnail = new FileThumbnail(context);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (null != thumbnail) {
                        // 重置文件
                        thumbnail.resetFile(anchor.getFile());
                        // 重置上下文
                        task.parentFileLabel.setContext(thumbnail.toJSON());

                        // 更新到数据库
                        if (!messageMap.containsKey(task.message.id)) {
                            storage = task.storage;
                            messageMap.put(task.message.id, task.message);
                        }
                    }
                }
            }

            this.fileCodes.remove(fileCode);
        }

        for (Message message : messageMap.values()) {
            storage.updateMessageAttachment(message.id, message.getAttachment());
        }
        messageMap.clear();
    }

    @Override
    public void handleFailure(ModuleError error, @Nullable FileAnchor anchor) {
        LogUtils.w(ThumbnailDownloadTask.class.getSimpleName(),
                "download thumbnail failed : " + anchor.getFileName());

        String fileCode = anchor.getFileCode();

        synchronized (this.fileCodes) {
            Iterator<ThumbnailDownloadTask> iter = this.taskList.iterator();
            while (iter.hasNext()) {
                ThumbnailDownloadTask task = iter.next();
                if (task.getFileCode().equals(fileCode)) {
                    iter.remove();
                }
            }
            this.fileCodes.remove(fileCode);
        }
    }
}
