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

package cube.filestorage.model;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cube.core.Module;
import cube.core.ModuleError;
import cube.core.Packet;
import cube.core.PipelineState;
import cube.core.handler.FailureHandler;
import cube.core.handler.PipelineHandler;
import cube.core.handler.StableFailureHandler;
import cube.filestorage.FileStorage;
import cube.filestorage.FileStorageAction;
import cube.filestorage.FileStorageEvent;
import cube.filestorage.FileStorageState;
import cube.filestorage.StructStorage;
import cube.filestorage.handler.DefaultDirectoryHandler;
import cube.filestorage.handler.DirectoryFileUploadHandler;
import cube.filestorage.handler.DirectoryHandler;
import cube.filestorage.handler.DirectoryListHandler;
import cube.filestorage.handler.FileItemListHandler;
import cube.filestorage.handler.FileListHandler;
import cube.filestorage.handler.StableUploadFileHandler;
import cube.util.LogUtils;
import cube.util.ObservableEvent;

/**
 * 文件层级结构描述。
 */
public class FileHierarchy {

    private final static String TAG = "FileHierarchy";

    private final FileStorage service;

    private final StructStorage storage;

    /**
     * 当前层级的根目录。
     */
    private Directory root;

    private List<FileAnchor> uploadingFiles;

    private List<FileAnchor> downloadingFiles;

    /**
     * 包含的目录。
     */
    private Map<Long, Directory> directoryMap;

    public FileHierarchy(FileStorage service, StructStorage storage, Directory root) {
        this.service = service;
        this.storage = storage;
        this.root = root;
        this.root.setHierarchy(this);
        this.directoryMap = new HashMap<>();
        this.uploadingFiles = new ArrayList<>();
        this.downloadingFiles = new ArrayList<>();
    }

    public Directory getRoot() {
        return this.root;
    }

    public List<FileAnchor> getUploadingFiles() {
        return this.uploadingFiles;
    }

    public List<FileAnchor> getDownloadingFiles() {
        return this.downloadingFiles;
    }

    public Directory retrieve(Long directoryId) {
        // 深度遍历
        return this.retrieve(this.root, directoryId.longValue());
    }

    private Directory retrieve(Directory current, long directoryId) {
        if (current.id.longValue() == directoryId) {
            return current;
        }

        // 遍历子目录
        Directory target = retrieveChildren(current, directoryId);
        return target;
    }

    private Directory retrieveChildren(Directory parent, long directoryId) {
        for (Directory child : parent.getChildren()) {
            Directory target = retrieve(child, directoryId);
            if (null != target) {
                return target;
            }
        }

        return null;
    }

    /**
     * 恢复文件。
     *
     * @param parentId
     * @param fileLabel
     * @return
     */
    public boolean restoreFileLabel(Long parentId, FileLabel fileLabel) {
        Directory directory = this.retrieve(parentId);
        if (null != directory) {
            if (directory.addFile(fileLabel)) {
                directory.setNumDirs(directory.numFiles() + 1);
                return true;
            }
        }

        return false;
    }

    /**
     * 恢复目录。
     *
     * @param directory
     */
    public void restoreDirectory(Directory directory) {
        Directory parent = this.retrieve(directory.getParentId());
        if (null == parent) {
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("root", this.root.id.longValue());
            payload.put("id", parent.id.longValue());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(FileStorageAction.ListDirs, payload);
        this.service.getPipeline().send(FileStorage.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != FileStorageState.Ok.code) {
                    return;
                }

                JSONObject data = packet.extractServiceData();
                try {
                    List<Directory> list = new ArrayList<>();
                    JSONArray array = data.getJSONArray("list");
                    for (int i = 0; i < array.length(); ++i) {
                        Directory dir = new Directory(array.getJSONObject(i));
                        // 添加子目录
                        parent.addChild(dir);

                        list.add(dir);

                        // 写入存储
                        storage.writeDirectory(dir);
                    }
                } catch (JSONException e) {
                    LogUtils.w(TAG, "#listDirectories", e);
                }
            }
        });
    }

    /**
     * 获取目录列表。
     *
     * @param directory
     * @param successHandler
     * @param failureHandler
     */
    protected void listDirectories(Directory directory, DirectoryListHandler successHandler, FailureHandler failureHandler) {
        List<Directory> subdirectories = this.storage.readSubdirectories(directory);
        for (Directory child : subdirectories) {
            // 添加
            directory.addChild(child);
        }

        if (subdirectories.size() == directory.numDirs()) {
            if (successHandler.isInMainThread()) {
                this.service.executeHandlerOnMainThread(() -> {
                    successHandler.handleDirectoryList(subdirectories);
                });
            }
            else {
                this.service.executeHandler(() -> {
                    successHandler.handleDirectoryList(subdirectories);
                });
            }
            return;
        }

        if (!this.service.getPipeline().isReady()) {
            ModuleError error = new ModuleError(FileStorage.NAME, FileStorageState.PipelineNotReady.code);
            this.service.execute(failureHandler, error);
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("root", this.root.id.longValue());
            payload.put("id", directory.id.longValue());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(FileStorageAction.ListDirs, payload);
        this.service.getPipeline().send(FileStorage.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(FileStorage.NAME, packet.state.code);
                    service.execute(failureHandler, error);
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != FileStorageState.Ok.code) {
                    ModuleError error = new ModuleError(FileStorage.NAME, stateCode);
                    service.execute(failureHandler, error);
                    return;
                }

                JSONObject data = packet.extractServiceData();
                try {
                    List<Directory> list = new ArrayList<>();
                    JSONArray array = data.getJSONArray("list");
                    for (int i = 0; i < array.length(); ++i) {
                        Directory dir = new Directory(array.getJSONObject(i));
                        // 添加子目录
                        directory.addChild(dir);

                        list.add(dir);

                        // 写入存储
                        storage.writeDirectory(dir);
                    }

                    if (successHandler.isInMainThread()) {
                        service.executeHandlerOnMainThread(() -> {
                            successHandler.handleDirectoryList(list);
                        });
                    }
                    else {
                        service.executeHandler(() -> {
                            successHandler.handleDirectoryList(list);
                        });
                    }
                } catch (JSONException e) {
                    LogUtils.w(TAG, "#listDirectories", e);
                }
            }
        });
    }

    /**
     * 获取文件列表。
     *
     * @param directory
     * @param beginIndex
     * @param endIndex
     * @param successHandler
     * @param failureHandler
     */
    protected void listFiles(Directory directory, int beginIndex, int endIndex, FileListHandler successHandler, FailureHandler failureHandler) {
        final List<FileLabel> files = this.storage.readFiles(directory);
        for (FileLabel fileLabel : files) {
            // 添加
            directory.addFile(fileLabel);
        }

        if (files.size() == directory.numFiles()) {
            if (successHandler.isInMainThread()) {
                this.service.executeHandlerOnMainThread(() -> {
                    successHandler.handleFileList(files);
                });
            }
            else {
                this.service.executeHandler(() -> {
                    successHandler.handleFileList(files);
                });
            }
            return;
        }

        if (!files.isEmpty()) {
            List<FileLabel> partList = new ArrayList<>();
            for (int i = beginIndex; i <= endIndex; ++i) {
                if (i < files.size()) {
                    partList.add(files.get(i));
                }
                else {
                    break;
                }
            }

            if (partList.size() == endIndex - beginIndex + 1) {
                if (successHandler.isInMainThread()) {
                    this.service.executeHandlerOnMainThread(() -> {
                        successHandler.handleFileList(partList);
                    });
                }
                else {
                    this.service.executeHandler(() -> {
                        successHandler.handleFileList(partList);
                    });
                }
                return;
            }
        }

        if (!this.service.getPipeline().isReady()) {
            ModuleError error = new ModuleError(FileStorage.NAME, FileStorageState.PipelineNotReady.code);
            this.service.execute(failureHandler, error);
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("root", this.root.id.longValue());
            payload.put("id", directory.id.longValue());
            payload.put("begin", beginIndex);
            payload.put("end", endIndex);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(FileStorageAction.ListFiles, payload);
        this.service.getPipeline().send(FileStorage.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(FileStorage.NAME, packet.state.code);
                    service.execute(failureHandler, error);
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != FileStorageState.Ok.code) {
                    ModuleError error = new ModuleError(FileStorage.NAME, stateCode);
                    service.execute(failureHandler, error);
                    return;
                }

                JSONObject data = packet.extractServiceData();
                try {
                    List<FileLabel> list = new ArrayList<>();
                    JSONArray array = data.getJSONArray("list");
                    for (int i = 0; i < array.length(); ++i) {
                        FileLabel fileLabel = new FileLabel(array.getJSONObject(i));
                        // 添加文件
                        if (directory.addFile(fileLabel)) {
                            list.add(fileLabel);
                        }

                        // 更新数据库
                        storage.writeFileLabel(directory, fileLabel);
                    }

                    if (successHandler.isInMainThread()) {
                        service.executeHandlerOnMainThread(() -> {
                            successHandler.handleFileList(list);
                        });
                    }
                    else {
                        service.executeHandler(() -> {
                            successHandler.handleFileList(list);
                        });
                    }
                } catch (JSONException e) {
                    LogUtils.w(TAG, "#listFiles", e);
                }
            }
        });
    }

    /**
     * 上传文件到指定目录。
     *
     * @param file
     * @param directory
     * @param successHandler
     * @param failureHandler
     */
    protected void uploadFile(File file, Directory directory, DirectoryFileUploadHandler successHandler, FailureHandler failureHandler) {
        FileAnchor anchor = new FileAnchor(file);

        // 记录需要上传的文件
        synchronized (this.uploadingFiles) {
            this.uploadingFiles.add(anchor);
        }

        if (!this.service.getPipeline().isReady()) {
            ModuleError error = new ModuleError(FileStorage.NAME, FileStorageState.PipelineNotReady.code);
            this.service.execute(failureHandler, error);
            return;
        }

        // 第一步，上传文件
        this.service.uploadFile(anchor, new StableUploadFileHandler() {
            @Override
            public void handleStarted(FileAnchor anchor) {
                // Nothing
            }

            @Override
            public void handleProcessing(FileAnchor anchor) {
                if (successHandler.isInMainThread()) {
                    service.executeHandlerOnMainThread(() -> {
                        successHandler.handleProgress(anchor, directory);
                    });
                }
                else {
                    service.executeHandler(() -> {
                        successHandler.handleProgress(anchor, directory);
                    });
                }
            }

            @Override
            public void handleSuccess(FileAnchor fileAnchor, FileLabel fileLabel) {
                synchronized (uploadingFiles) {
                    uploadingFiles.remove(anchor);
                }

                // 第二步，放置文件到目录
                insertFile(fileLabel, directory, new DefaultDirectoryHandler(false) {
                    @Override
                    public void handleDirectory(Directory directory) {
                        if (successHandler.isInMainThread()) {
                            service.executeHandlerOnMainThread(() -> {
                                successHandler.handleComplete(fileLabel, directory);
                            });
                        }
                        else {
                            service.executeHandler(() -> {
                                successHandler.handleComplete(fileLabel, directory);
                            });
                        }
                    }
                }, new StableFailureHandler() {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {
                        service.execute(failureHandler, error);
                    }
                });
            }

            @Override
            public void handleFailure(ModuleError error, @Nullable FileAnchor anchor) {
                service.execute(failureHandler, error);
            }
        });
    }

    private void insertFile(FileLabel fileLabel, Directory directory, DirectoryHandler successHandler, FailureHandler failureHandler) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("root", this.root.id.longValue());
            payload.put("dirId", directory.id.longValue());
            payload.put("fileCode", fileLabel.getFileCode());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(FileStorageAction.InsertFile, payload);
        this.service.getPipeline().send(FileStorage.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(FileStorage.NAME, packet.state.code);
                    service.execute(failureHandler, error);
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != FileStorageState.Ok.code) {
                    ModuleError error = new ModuleError(FileStorage.NAME, stateCode);
                    service.execute(failureHandler, error);
                    return;
                }

                JSONObject data = packet.extractServiceData();
                try {
                    // 更新数据
                    Directory response = new Directory(data.getJSONObject("directory"));
                    directory.update(response);
                    directory.addFile(fileLabel);

                    // 更新目录数据
                    storage.writeDirectory(directory);
                    // 更新文件数据
                    storage.writeFileLabel(directory, fileLabel);
                } catch (JSONException e) {
                    LogUtils.w(TAG, "#insertFile", e);
                }

                service.executeHandler(() -> {
                    successHandler.handleDirectory(directory);
                });
            }
        });
    }

    /**
     * 删除指定目录下的文件。
     *
     * @param workingDirectory
     * @param fileLabel
     * @param successHandler
     * @param failureHandler
     */
    protected void deleteFile(Directory workingDirectory, FileLabel fileLabel, DirectoryHandler successHandler, FailureHandler failureHandler) {
        if (!this.service.getPipeline().isReady()) {
            ModuleError error = new ModuleError(FileStorage.NAME, FileStorageState.PipelineNotReady.code);
            this.service.execute(failureHandler, error);
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("root", this.root.id.longValue());
            payload.put("workingId", workingDirectory.id.longValue());

            JSONArray fileList = new JSONArray();
            fileList.put(fileLabel.getFileCode());
            payload.put("fileList", fileList);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(FileStorageAction.DeleteFile, payload);
        this.service.getPipeline().send(FileStorage.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(FileStorage.NAME, packet.state.code);
                    error.data = workingDirectory;
                    service.execute(failureHandler, error);
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != FileStorageState.Ok.code) {
                    ModuleError error = new ModuleError(FileStorage.NAME, stateCode);
                    error.data = workingDirectory;
                    service.execute(failureHandler, error);
                    return;
                }

                try {
                    JSONObject data = packet.extractServiceData();

                    ArrayList<FileLabel> fileLabelList = new ArrayList<>();
                    JSONArray deletedList = data.getJSONArray("deletedList");
                    for (int i = 0; i < deletedList.length(); ++i) {
                        FileLabel label = new FileLabel(deletedList.getJSONObject(i));
                        fileLabelList.add(label);
                    }

                    for (FileLabel label : fileLabelList) {
                        workingDirectory.removeFile(label);
                        storage.deleteFile(workingDirectory, label);
                    }
                    fileLabelList.clear();

                    Directory response = new Directory(data.getJSONObject("workingDir"));
                    workingDirectory.update(response);

                    storage.writeDirectory(workingDirectory);

                    if (successHandler.isInMainThread()) {
                        service.executeHandlerOnMainThread(() -> {
                            successHandler.handleDirectory(workingDirectory);
                        });
                    }
                    else {
                        service.executeHandler(() -> {
                            successHandler.handleDirectory(workingDirectory);
                        });
                    }

                    service.executeHandler(() -> {
                        ObservableEvent event = new ObservableEvent(FileStorageEvent.DeleteFile, workingDirectory, fileLabel);
                        service.notifyObservers(event);
                    });
                } catch (JSONException e) {
                    LogUtils.w(TAG, "#deleteFile", e);
                }
            }
        });
    }

    /**
     * 新建目录。
     *
     * @param workingDirectory
     * @param directoryName
     * @param successHandler
     * @param failureHandler
     */
    protected void newDirectory(Directory workingDirectory, String directoryName, DirectoryHandler successHandler, FailureHandler failureHandler) {
        if (!this.service.getPipeline().isReady()) {
            ModuleError error = new ModuleError(FileStorage.NAME, FileStorageState.PipelineNotReady.code);
            this.service.execute(failureHandler, error);
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("root", this.root.id.longValue());
            payload.put("workingId", workingDirectory.id.longValue());
            payload.put("dirName", directoryName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(FileStorageAction.NewDir, payload);
        this.service.getPipeline().send(FileStorage.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(FileStorage.NAME, packet.state.code);
                    error.data = workingDirectory;
                    service.execute(failureHandler, error);
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != FileStorageState.Ok.code) {
                    ModuleError error = new ModuleError(FileStorage.NAME, stateCode);
                    error.data = workingDirectory;
                    service.execute(failureHandler, error);
                    return;
                }

                try {
                    Directory newDirectory = new Directory(packet.extractServiceData());
                    workingDirectory.addChild(newDirectory);

                    workingDirectory.resetLast(System.currentTimeMillis());
                    workingDirectory.setNumDirs(workingDirectory.numDirs() + 1);

                    storage.writeDirectory(workingDirectory);
                    storage.writeDirectory(newDirectory);

                    if (successHandler.isInMainThread()) {
                        service.executeHandlerOnMainThread(() -> {
                            successHandler.handleDirectory(newDirectory);
                        });
                    }
                    else {
                        service.executeHandler(() -> {
                            successHandler.handleDirectory(newDirectory);
                        });
                    }

                    service.executeHandler(() -> {
                        ObservableEvent event = new ObservableEvent(FileStorageEvent.NewDirectory, workingDirectory, newDirectory);
                        service.notifyObservers(event);
                    });
                } catch (JSONException e) {
                    LogUtils.w(TAG, "#newDirectory", e);
                }
            }
        });
    }

    /**
     * 删除指定目录。
     *
     * @param workingDirectory
     * @param targetDirectory
     * @param recursive
     * @param successHandler
     * @param failureHandler
     */
    protected void deleteDirectory(Directory workingDirectory, Directory targetDirectory, boolean recursive, DirectoryHandler successHandler, FailureHandler failureHandler) {
        if (!this.service.getPipeline().isReady()) {
            ModuleError error = new ModuleError(FileStorage.NAME, FileStorageState.PipelineNotReady.code);
            this.service.execute(failureHandler, error);
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("root", this.root.id.longValue());
            payload.put("workingId", workingDirectory.id.longValue());

            JSONArray dirIdArray = new JSONArray();
            dirIdArray.put(targetDirectory.id.longValue());
            payload.put("dirList", dirIdArray);

            payload.put("recursive", recursive);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(FileStorageAction.DeleteDir, payload);
        this.service.getPipeline().send(FileStorage.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(FileStorage.NAME, packet.state.code);
                    error.data = targetDirectory;
                    service.execute(failureHandler, error);
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != FileStorageState.Ok.code) {
                    ModuleError error = new ModuleError(FileStorage.NAME, stateCode);
                    error.data = targetDirectory;
                    service.execute(failureHandler, error);
                    return;
                }

                try {
                    JSONObject data = packet.extractServiceData();

                    ArrayList<Directory> directoryList = new ArrayList<>();
                    JSONArray deletedList = data.getJSONArray("deletedList");
                    for (int i = 0; i < deletedList.length(); ++i) {
                        directoryList.add(new Directory(deletedList.getJSONObject(i)));
                    }

                    Directory response = new Directory(data.getJSONObject("workingDir"));
                    workingDirectory.update(response);

                    for (Directory deleted : directoryList) {
                        workingDirectory.removeChild(deleted);
                        storage.deleteDirectory(deleted);
                    }

                    storage.writeDirectory(workingDirectory);

                    if (successHandler.isInMainThread()) {
                        service.executeHandlerOnMainThread(() -> {
                            successHandler.handleDirectory(workingDirectory);
                        });
                    }
                    else {
                        service.executeHandler(() -> {
                            successHandler.handleDirectory(workingDirectory);
                        });
                    }

                    service.executeHandler(() -> {
                        ObservableEvent event = new ObservableEvent(FileStorageEvent.DeleteDirectory, workingDirectory, targetDirectory);
                        service.notifyObservers(event);
                    });
                } catch (JSONException e) {
                    LogUtils.w(TAG, "#deleteDirectory", e);
                }
            }
        });
    }

    /**
     * 重命名目录。
     *
     * @param targetDirectory
     * @param directoryName
     * @param successHandler
     * @param failureHandler
     */
    protected void renameDirectory(Directory targetDirectory, String directoryName, DirectoryHandler successHandler, FailureHandler failureHandler) {
        if (!this.service.getPipeline().isReady()) {
            ModuleError error = new ModuleError(FileStorage.NAME, FileStorageState.PipelineNotReady.code);
            this.service.execute(failureHandler, error);
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("root", this.root.id.longValue());
            payload.put("workingId", targetDirectory.id.longValue());
            payload.put("dirName", directoryName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(FileStorageAction.RenameDir, payload);
        this.service.getPipeline().send(FileStorage.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(FileStorage.NAME, packet.state.code);
                    error.data = targetDirectory;
                    service.execute(failureHandler, error);
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != FileStorageState.Ok.code) {
                    ModuleError error = new ModuleError(FileStorage.NAME, stateCode);
                    error.data = targetDirectory;
                    service.execute(failureHandler, error);
                    return;
                }

                try {
                    Directory response = new Directory(packet.extractServiceData());
                    targetDirectory.update(response);

                    storage.writeDirectory(targetDirectory);

                    if (successHandler.isInMainThread()) {
                        service.executeHandlerOnMainThread(() -> {
                            successHandler.handleDirectory(targetDirectory);
                        });
                    }
                    else {
                        service.executeHandler(() -> {
                            successHandler.handleDirectory(targetDirectory);
                        });
                    }

                    service.executeHandler(() -> {
                        ObservableEvent event = new ObservableEvent(FileStorageEvent.RenameDirectory, targetDirectory);
                        service.notifyObservers(event);
                    });
                } catch (JSONException e) {
                    LogUtils.w(TAG, "#renameDirectory", e);
                }
            }
        });
    }

    protected void handle(FileItemListHandler handler, List<FileItem> list) {
        if (handler.isInMainThread()) {
            this.service.executeHandlerOnMainThread(() -> {
                handler.handleFileItemList(list);
            });
        }
        else {
            this.service.executeHandler(() -> {
                handler.handleFileItemList(list);
            });
        }
    }

    protected void handle(FailureHandler failureHandler, ModuleError error) {
        if (failureHandler.isInMainThread()) {
            this.service.executeHandlerOnMainThread(() -> {
                failureHandler.handleFailure(this.service, error);
            });
        }
        else {
            this.service.executeHandler(() -> {
                failureHandler.handleFailure(this.service, error);
            });
        }
    }
}
