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

import android.os.Build;
import android.util.MutableInt;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import cube.contact.ContactService;
import cube.contact.ContactServiceEvent;
import cube.contact.model.Self;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.Packet;
import cube.core.PipelineState;
import cube.core.handler.PipelineHandler;
import cube.filestorage.handler.StableFileLabelHandler;
import cube.filestorage.handler.UploadFileHandler;
import cube.filestorage.model.FileAnchor;
import cube.filestorage.model.FileLabel;
import cube.util.LogUtils;
import cube.util.ObservableEvent;
import cube.util.Observer;

/**
 * 文件存储服务。
 */
public class FileStorage extends Module implements Observer, UploadQueue.UploadQueueListener {

    public final static String NAME = "FileStorage";

    private Self self;

    private String fileURL = "http://cube.shixincube.com/filestorage/file/";

    private String fileSecureURL = "https://cube.shixincube.com/filestorage/file/";

    private MutableInt fileBlockSize = new MutableInt(10 * 1024);

    private StructStorage storage;

    private UploadQueue uploadQueue;

    public FileStorage() {
        super(NAME);
    }

    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }

        ContactService contactService = (ContactService) this.kernel.getModule(ContactService.NAME);
        contactService.attachWithName(ContactServiceEvent.SelfReady, this);
        this.self = contactService.getSelf();

        if (null != this.self) {
            this.storage = new StructStorage();
            this.storage.open(getContext(), this.self.id, this.self.domain);
        }

        this.uploadQueue = new UploadQueue(this, this.fileBlockSize);
        this.uploadQueue.setListener(this);

        return true;
    }

    @Override
    public void stop() {
        super.stop();

        ContactService contactService = (ContactService) this.kernel.getModule(ContactService.NAME);
        contactService.detachWithName(ContactServiceEvent.SelfReady, this);

        if (null != this.storage) {
            this.storage.close();
            this.storage = null;
        }
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    protected void config(@Nullable JSONObject configData) {
        try {
            this.fileURL = configData.getString("fileURL");
            this.fileSecureURL = configData.getString("fileSecureURL");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (Build.SERIAL.contains("unknown")) {
            // FIXME 以下判断仅用于测试，Release 时务必使用域名
            // 模拟器里将 127.0.0.1 修改为 10.0.2.2
            this.fileURL = this.fileURL.replace("127.0.0.1", "10.0.2.2");
            this.fileSecureURL = this.fileSecureURL.replace("127.0.0.1", "10.0.2.2");
        }
    }

    /**
     * 上传到默认目录。
     *
     * @param file
     * @param handler
     */
    public void uploadFile(File file, UploadFileHandler handler) {
        if (!this.hasStarted() || null == this.self) {
            if (handler.isInMainThread()) {
                this.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        ModuleError error = new ModuleError(NAME, FileStorageState.NotReady.code);
                        handler.handleFailure(error, null);
                    }
                });
            }
            else {
                this.execute(new Runnable() {
                    @Override
                    public void run() {
                        ModuleError error = new ModuleError(NAME, FileStorageState.NotReady.code);
                        handler.handleFailure(error, null);
                    }
                });
            }
            return;
        }

        try {
            FileAnchor fileAnchor = new FileAnchor(file);
            fileAnchor.setUploadFileHandler(handler);
            fileAnchor.bindInputStream(new FileInputStream(file));

            // 将文件锚点添加到上传队列
            this.uploadQueue.enqueue(fileAnchor);
        } catch (IOException e) {
            LogUtils.w(this.getClass().getSimpleName(), e);
        }
    }

    /**
     * 上传文件数据到默认目录。
     *
     * @param filename
     * @param inputStream
     * @param handler
     */
    public void uploadFile(String filename, InputStream inputStream, UploadFileHandler handler) {
        if (!this.hasStarted() || null == this.self) {
            if (handler.isInMainThread()) {
                this.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        ModuleError error = new ModuleError(NAME, FileStorageState.NotReady.code);
                        handler.handleFailure(error, null);
                    }
                });
            }
            else {
                this.execute(new Runnable() {
                    @Override
                    public void run() {
                        ModuleError error = new ModuleError(NAME, FileStorageState.NotReady.code);
                        handler.handleFailure(error, null);
                    }
                });
            }
            return;
        }

        long lastModified = System.currentTimeMillis();
        try {
            // 文件大小
            long fileSize = inputStream.available();

            FileAnchor fileAnchor = new FileAnchor(filename, fileSize, lastModified);
            fileAnchor.setUploadFileHandler(handler);
            fileAnchor.bindInputStream(inputStream);

            // 将文件锚点添加到上传队列
            this.uploadQueue.enqueue(fileAnchor);
        } catch (IOException e) {
            LogUtils.w(this.getClass().getSimpleName(), e);
        }
    }

    protected Self getSelf() {
        return this.self;
    }

    protected String getServiceURL() {
        return this.fileURL;
    }

    protected String getTokenCode() {
        return this.getAuthToken().code;
    }

    @Override
    protected void execute(Runnable task) {
        super.execute(task);
    }

    /**
     * 获取文件标签。
     *
     * @param fileCode
     * @param handler
     */
    protected void getRemoteFileLabel(String fileCode, StableFileLabelHandler handler) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("fileCode", fileCode);
        } catch (JSONException e) {
            // Nothing
        }
        Packet requestPacket = new Packet(FileStorageAction.GetFile, payload);
        this.pipeline.send(FileStorage.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            ModuleError error = new ModuleError(FileStorage.NAME, packet.state.code, fileCode);
                            handler.handleFailure(error, null);
                        }
                    });
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != FileStorageState.Ok.code) {
                    // 判断状态码
                    if (stateCode == FileStorageState.Writing.code) {
                        // 正在写入文件，可以延迟后再试
                        executeDelayed(new Runnable() {
                            @Override
                            public void run() {
                                getRemoteFileLabel(fileCode, handler);
                            }
                        }, 500);
                    }
                    else {
                        execute(new Runnable() {
                            @Override
                            public void run() {
                                ModuleError error = new ModuleError(FileStorage.NAME, stateCode, fileCode);
                                handler.handleFailure(error, null);
                            }
                        });
                    }

                    return;
                }

                try {
                    FileLabel fileLabel = new FileLabel(packet.extractServiceData());
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            handler.handleSuccess(fileLabel);
                        }
                    });
                } catch (JSONException e) {
                    LogUtils.w(FileStorage.class.getSimpleName(), e);
                }
            }
        });
    }

    @Override
    public void onUploadStarted(FileAnchor fileAnchor) {
        UploadFileHandler uploadHandler = fileAnchor.getUploadFileHandler();
        if (null != uploadHandler) {
            if (uploadHandler.isInMainThread()) {
                this.executeOnMainThread(() -> {
                    uploadHandler.handleStarted(fileAnchor);
                });
            }
            else {
                this.execute(() -> {
                    uploadHandler.handleStarted(fileAnchor);
                });
            }
        }
    }

    @Override
    public void onUploading(FileAnchor fileAnchor) {
        UploadFileHandler uploadHandler = fileAnchor.getUploadFileHandler();
        if (null != uploadHandler) {
            if (uploadHandler.isInMainThread()) {
                this.executeOnMainThread(() -> {
                    uploadHandler.handleProcessing(fileAnchor);
                });
            }
            else {
                this.execute(() -> {
                    uploadHandler.handleProcessing(fileAnchor);
                });
            }
        }
    }

    @Override
    public void onUploadCompleted(FileAnchor fileAnchor) {
        // 上传完成之后获取文件标签

        final UploadFileHandler uploadHandler = fileAnchor.getUploadFileHandler();
        if (null == uploadHandler) {
            return;
        }

        PostTask postTask = (anchor, label) -> {
            // 设置文件路径
            label.setFilePath(anchor.filePath);

            // 数据入库
            this.storage.writeFileLabel(label);
        };

        this.execute(new Runnable() {
            @Override
            public void run() {
                // 获取文件标签
                getRemoteFileLabel(fileAnchor.getFileCode(), new StableFileLabelHandler() {
                    @Override
                    public void handleSuccess(FileLabel fileLabel) {
                        // 后处理
                        postTask.process(fileAnchor, fileLabel);

                        if (uploadHandler.isInMainThread()) {
                            executeOnMainThread(() -> {
                                uploadHandler.handleSuccess(fileLabel);
                            });
                        }
                        else {
                            execute(() -> {
                                uploadHandler.handleSuccess(fileLabel);
                            });
                        }
                    }

                    @Override
                    public void handleFailure(ModuleError error, @Nullable FileLabel fileLabel) {
                        // 延迟之后再试
                        executeDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // 获取文件标签
                                getRemoteFileLabel(fileAnchor.getFileCode(), new StableFileLabelHandler() {
                                    @Override
                                    public void handleSuccess(FileLabel fileLabel) {
                                        // 后处理
                                        postTask.process(fileAnchor, fileLabel);

                                        if (uploadHandler.isInMainThread()) {
                                            executeOnMainThread(() -> {
                                                uploadHandler.handleSuccess(fileLabel);
                                            });
                                        }
                                        else {
                                            execute(() -> {
                                                uploadHandler.handleSuccess(fileLabel);
                                            });
                                        }
                                    }

                                    @Override
                                    public void handleFailure(ModuleError error, @Nullable FileLabel fileLabel) {
                                        if (uploadHandler.isInMainThread()) {
                                            executeOnMainThread(() -> {
                                                uploadHandler.handleFailure(error, fileAnchor);
                                            });
                                        }
                                        else {
                                            execute(() -> {
                                                uploadHandler.handleFailure(error, fileAnchor);
                                            });
                                        }
                                    }
                                });
                            }
                        }, 500);
                    }
                });
            }
        });
    }

    @Override
    public void onUploadFailed(FileAnchor fileAnchor, int errorCode) {
        UploadFileHandler uploadHandler = fileAnchor.getUploadFileHandler();
        if (null != uploadHandler) {
            if (uploadHandler.isInMainThread()) {
                this.executeOnMainThread(() -> {
                    ModuleError error = new ModuleError(NAME, errorCode);
                    uploadHandler.handleFailure(error, fileAnchor);
                });
            }
            else {
                this.execute(() -> {
                    ModuleError error = new ModuleError(NAME, errorCode);
                    uploadHandler.handleFailure(error, fileAnchor);
                });
            }
        }
    }

    @Override
    public void update(ObservableEvent event) {
        if (event.getName().equals(ContactServiceEvent.SelfReady)) {
            this.self = ((ContactService) event.getSubject()).getSelf();
            if (null == this.storage) {
                this.storage = new StructStorage();
                this.storage.open(getContext(), this.self.id, this.self.domain);
            }
        }
    }

    interface PostTask {
        void process(FileAnchor fileAnchor, FileLabel fileLabel);
    }
}
