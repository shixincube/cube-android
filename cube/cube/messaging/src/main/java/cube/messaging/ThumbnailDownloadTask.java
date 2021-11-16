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

import cube.core.ModuleError;
import cube.fileprocessor.model.FileThumbnail;
import cube.filestorage.FileStorage;
import cube.filestorage.handler.StableDownloadFileHandler;
import cube.filestorage.model.FileAnchor;
import cube.filestorage.model.FileLabel;
import cube.messaging.model.Message;
import cube.util.LogUtils;

/**
 * 缩略图下载任务。
 */
public class ThumbnailDownloadTask implements Runnable {

    public final FileStorage fileStorageService;

    public final MessagingStorage storage;

    public final Message message;

    public final FileLabel parentFileLabel;

    public final FileThumbnail thumbnail;

    public ThumbnailDownloadTask(FileStorage fileStorageService, MessagingStorage storage,
                                 Message message, FileLabel parentFileLabel, FileThumbnail thumbnail) {
        this.fileStorageService = fileStorageService;
        this.storage = storage;
        this.message = message;
        this.parentFileLabel = parentFileLabel;
        this.thumbnail = thumbnail;
    }

    public String getFileCode() {
        return this.thumbnail.getFileLabel().getFileCode();
    }

    @Override
    public void run() {
        this.fileStorageService.downloadFile(this.thumbnail.getFileLabel(), new StableDownloadFileHandler() {
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
                JSONObject context = parentFileLabel.getContext();
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
                    parentFileLabel.setContext(thumbnail.toJSON());
                    // 更新到数据库
                    storage.updateMessageAttachment(message.id, message.getAttachment());
                }
            }

            @Override
            public void handleFailure(ModuleError error, @Nullable FileAnchor anchor) {
                LogUtils.w(ThumbnailDownloadTask.class.getSimpleName(),
                        "download thumbnail failed : " + thumbnail.getFileLabel().getFileName());
            }
        });
    }
}
