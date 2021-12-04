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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cube.core.ModuleError;
import cube.core.Packet;
import cube.core.PipelineState;
import cube.core.handler.FailureHandler;
import cube.core.handler.PipelineHandler;
import cube.filestorage.FileStorage;
import cube.filestorage.FileStorageAction;
import cube.filestorage.FileStorageState;
import cube.filestorage.StructStorage;
import cube.filestorage.handler.DirectoryListHandler;
import cube.filestorage.handler.FileItemListHandler;
import cube.filestorage.handler.FileListHandler;
import cube.util.LogUtils;

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

    /**
     * 当前层级包含的目录。
     */
    private Map<Long, Directory> directoryMap;

    public FileHierarchy(FileStorage service, StructStorage storage, Directory root) {
        this.service = service;
        this.storage = storage;
        this.root = root;
        this.root.setHierarchy(this);
        this.directoryMap = new HashMap<>();
    }

    public Directory getRoot() {
        return this.root;
    }

    /**
     *
     * @param directory
     * @param successHandler
     * @param failureHandler
     */
    protected void listDirectories(Directory directory, DirectoryListHandler successHandler, FailureHandler failureHandler) {
        List<Directory> subdirectories = this.storage.readSubdirectories(directory);
        if (subdirectories.size() == directory.numDirs()) {
            for (Directory child : subdirectories) {
                // 添加
                directory.addChildren(child);
            }

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
                        directory.addChildren(dir);

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
                        directory.addFile(fileLabel);

                        list.add(fileLabel);

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
}
