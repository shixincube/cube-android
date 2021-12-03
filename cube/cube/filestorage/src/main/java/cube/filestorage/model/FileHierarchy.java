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

import java.util.HashMap;
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
import cube.filestorage.handler.FileItemListHandler;
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

    protected void listDirectories(Directory directory, FileItemListHandler successHandler, FailureHandler failureHandler) {
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
                    JSONArray array = data.getJSONArray("list");
                    for (int i = 0; i < array.length(); ++i) {
                        Directory dir = new Directory(array.getJSONObject(i));
                        directory.addChildren(dir);
                    }
                } catch (JSONException e) {
                    LogUtils.w(TAG, "#listFileItems", e);
                }
            }
        });
    }
}
