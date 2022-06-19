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
import android.os.Environment;
import android.util.MutableInt;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import cube.contact.ContactService;
import cube.contact.ContactServiceEvent;
import cube.contact.model.Self;
import cube.core.KernelConfig;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.Packet;
import cube.core.Pipeline;
import cube.core.PipelineState;
import cube.core.handler.FailureHandler;
import cube.core.handler.PipelineHandler;
import cube.core.handler.StableFailureHandler;
import cube.filestorage.handler.DefaultDirectoryHandler;
import cube.filestorage.handler.DirectoryHandler;
import cube.filestorage.handler.DownloadFileHandler;
import cube.filestorage.handler.FileItemListHandler;
import cube.filestorage.handler.MutableDirectory;
import cube.filestorage.handler.SearchResultHandler;
import cube.filestorage.handler.StableFileLabelHandler;
import cube.filestorage.handler.TrashHandler;
import cube.filestorage.handler.UploadFileHandler;
import cube.filestorage.model.Directory;
import cube.filestorage.model.FileAnchor;
import cube.filestorage.model.FileHierarchy;
import cube.filestorage.model.FileItem;
import cube.filestorage.model.FileLabel;
import cube.filestorage.model.SearchFilter;
import cube.filestorage.model.SearchResultItem;
import cube.filestorage.model.Trash;
import cube.filestorage.model.TrashDirectory;
import cube.filestorage.model.TrashFile;
import cube.util.LogUtils;
import cube.util.ObservableEvent;
import cube.util.Observer;

/**
 * 文件存储服务。
 */
public class FileStorage extends Module implements Observer, UploadQueue.UploadQueueListener, DownloadQueue.DownloadQueueListener {

    public final static String NAME = "FileStorage";

    private final static String TAG = FileStorage.class.getSimpleName();

    /** 阻塞调用方法的超时时间。 */
    private final long blockingTimeout = 10 * 1000;

    private Self self;

    private String fileURL = "http://cube.shixincube.com/filestorage/file/";

    private String fileSecureURL = "https://cube.shixincube.com/filestorage/file/";

    private MutableInt fileBlockSize = new MutableInt(64 * 1024);

    private String fileCachePath;

    private UploadQueue uploadQueue;

    private DownloadQueue downloadQueue;

    /**
     * 结构存储器。
     */
    private StructStorage storage;

    /**
     * 文件层级管理器。
     */
    private ConcurrentHashMap<Long, FileHierarchy> fileHierarchyMap;

    /**
     * 目录事件监听。
     */
    private List<DirectoryListener> directoryListeners;

    /**
     * 废弃的文件项。
     */
    private ConcurrentHashMap<Long, LinkedList<FileItem>> trashItemMap;

    public FileStorage() {
        super(NAME);
        this.fileHierarchyMap = new ConcurrentHashMap<>();
        this.trashItemMap = new ConcurrentHashMap<>();
        this.directoryListeners = new ArrayList<>();
    }

    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }

        StringBuilder buf = new StringBuilder();

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            buf.append(this.getContext().getExternalFilesDir("cube_files").getAbsoluteFile());
        }
        else {
            buf.append(this.getContext().getFilesDir());
            buf.append(File.separator);
            buf.append("cube_files");
        }
        buf.append(File.separator);

        this.fileCachePath = buf.toString();
        LogUtils.d(TAG, "Cube file dir: " + this.fileCachePath);

        File dir = new File(this.fileCachePath);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }

        if (!dir.exists()) {
            LogUtils.e(TAG, "Can NOT create file storage dir: " + dir.getPath());
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

        this.downloadQueue = new DownloadQueue(this, this.fileBlockSize);
        this.downloadQueue.setListener(this);

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

        this.fileHierarchyMap.clear();
        this.trashItemMap.clear();
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
        else {
            KernelConfig config = this.getKernel().getConfig();
            this.fileURL = this.fileURL.replace("127.0.0.1", config.address);
            this.fileSecureURL = this.fileSecureURL.replace("127.0.0.1", config.address);
        }
    }

    /**
     * 获取文件缓存路径。
     *
     * @return 返回文件缓存路径。
     */
    public String getFileCachePath() {
        return this.fileCachePath;
    }

    @Override
    public Pipeline getPipeline() {
        return this.pipeline;
    }

    /**
     * <b>Non-public API</b>
     * @param task
     */
    public void executeHandler(Runnable task) {
        super.execute(task);
    }

    /**
     * <b>Non-public API</b>
     * @param task
     */
    public void executeHandlerOnMainThread(Runnable task) {
        super.executeOnMainThread(task);
    }

    public void addDirectoryListener(DirectoryListener listener) {
        synchronized (this.directoryListeners) {
            if (!this.directoryListeners.contains(listener)) {
                this.directoryListeners.add(listener);
            }
        }
    }

    public void removeDirectoryListener(DirectoryListener listener) {
        synchronized (this.directoryListeners) {
            this.directoryListeners.remove(listener);
        }
    }

    /**
     * 获取文件层级管理器。
     *
     * @return 返回文件层级管理器。
     */
    public Directory getSelfRoot() {
        int count = 500;
        while (null == this.self || null == this.storage) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            --count;
            if (count <= 0) {
                break;
            }
        }

        if (null == this.self || null == this.storage) {
            return null;
        }

        FileHierarchy hierarchy = this.fileHierarchyMap.get(this.self.id);
        if (null != hierarchy) {
            return hierarchy.getRoot();
        }

        Directory directory = this.storage.readDirectory(this.self.id);
        if (null != directory) {
            hierarchy = new FileHierarchy(this, this.storage, directory);
            this.fileHierarchyMap.put(this.self.id, hierarchy);
            return hierarchy.getRoot();
        }

        MutableDirectory mutableDirectory = new MutableDirectory();

        // 从服务器上更新
        this.getRoot(this.self.id, new DefaultDirectoryHandler(false) {
            @Override
            public void handleDirectory(Directory directory) {
                mutableDirectory.directory = directory;

                synchronized (mutableDirectory) {
                    mutableDirectory.notify();
                }
            }
        }, new StableFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                synchronized (mutableDirectory) {
                    mutableDirectory.notify();
                }
            }
        });

        synchronized (mutableDirectory) {
            try {
                mutableDirectory.wait(this.blockingTimeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return mutableDirectory.directory;
    }

    /**
     * 获取指定 ID 的根目录。
     *
     * @param id
     * @param successHandler
     * @param failureHandler
     */
    private void getRoot(Long id, DirectoryHandler successHandler, FailureHandler failureHandler) {
        if (!this.pipeline.isReady()) {
            ModuleError error = new ModuleError(NAME, FileStorageState.PipelineNotReady.code);
            execute(failureHandler, error);
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("id", id.longValue());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(FileStorageAction.GetRoot, payload);
        this.pipeline.send(FileStorage.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(NAME, packet.state.code);
                    execute(failureHandler, error);
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != FileStorageState.Ok.code) {
                    ModuleError error = new ModuleError(NAME, stateCode);
                    execute(failureHandler, error);
                    return;
                }

                try {
                    Directory directory = new Directory(packet.extractServiceData());
                    FileHierarchy hierarchy = new FileHierarchy(FileStorage.this, storage, directory);
                    fileHierarchyMap.put(id, hierarchy);

                    // 更新数据库
                    storage.writeDirectory(directory);

                    if (successHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            successHandler.handleDirectory(directory);
                        });
                    }
                    else {
                        execute(() -> {
                            successHandler.handleDirectory(directory);
                        });
                    }
                } catch (JSONException e) {
                    LogUtils.w(TAG, "#getRoot", e);
                }
            }
        });
    }

    /**
     * 搜索当前联系的文件。该方法会遍历所有子目录。
     *
     * @param filter
     * @param handler
     */
    public void searchSelfFile(SearchFilter filter, SearchResultHandler handler) {
        if (!this.pipeline.isReady()) {
            if (handler.isInMainThread()) {
                executeOnMainThread(() -> {
                    handler.handleSearchResult(new ArrayList<>());
                });
            }
            else {
                execute(() -> {
                    handler.handleSearchResult(new ArrayList<>());
                });
            }
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("root", this.self.id.longValue());
            payload.put("filter", filter.toJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(FileStorageAction.SearchFile, payload);
        this.pipeline.send(FileStorage.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    if (handler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            handler.handleSearchResult(new ArrayList<>());
                        });
                    }
                    else {
                        execute(() -> {
                            handler.handleSearchResult(new ArrayList<>());
                        });
                    }
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != FileStorageState.Ok.code) {
                    if (handler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            handler.handleSearchResult(new ArrayList<>());
                        });
                    }
                    else {
                        execute(() -> {
                            handler.handleSearchResult(new ArrayList<>());
                        });
                    }
                    return;
                }

                try {
                    List<SearchResultItem> itemList = new ArrayList<>();

                    JSONArray result = packet.extractServiceData().getJSONArray("result");
                    for (int i = 0, len = result.length(); i < len; ++i) {
                        SearchResultItem item = new SearchResultItem(result.getJSONObject(i));
                        itemList.add(item);
                    }

                    if (handler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            handler.handleSearchResult(itemList);
                        });
                    }
                    else {
                        execute(() -> {
                            handler.handleSearchResult(itemList);
                        });
                    }
                } catch (JSONException e) {
                    LogUtils.w(TAG, "#searchSelfFile", e);
                }
            }
        });
    }

    /**
     * 获取当前联系人的所有废弃文件项。包括废弃的目录和文件。
     *
     * @param handler 指定操作回调句柄。
     */
    public void getSelfTrashFileItems(FileItemListHandler handler) {
        if (null == this.getSelfRoot()) {
            if (handler.isInMainThread()) {
                executeOnMainThread(() -> {
                    handler.handleFileItemList(new ArrayList<>());
                });
            }
            else {
                execute(() -> {
                    handler.handleFileItemList(new ArrayList<>());
                });
            }
            return;
        }

        final LinkedList<FileItem> trashItemList = this.trashItemMap.get(this.getSelfRoot().id);
        if (null == trashItemList) {
            LinkedList<FileItem> newList = new LinkedList<>();
            this.trashItemMap.put(this.getSelfRoot().id, newList);

            execute(() -> {
                LinkedList<FileItem> trashItems = this.trashItemMap.get(this.getSelfRoot().id);

                List<TrashDirectory> directoryList = storage.readTrashDirectories();
                for (TrashDirectory directory : directoryList) {
                    FileItem item = new FileItem(directory);
                    synchronized (trashItems) {
                        trashItems.add(item);
                    }
                }

                List<TrashFile> fileList = storage.readTrashFiles();
                for (TrashFile file : fileList) {
                    FileItem item = new FileItem(file);
                    synchronized (trashItems) {
                        trashItems.add(item);
                    }
                }

                if (!trashItems.isEmpty()) {
                    synchronized (trashItems) {
                        // 排序
                        sortFileItemsBySortableTime(trashItems);
                    }

                    if (handler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            handler.handleFileItemList(trashItems);
                        });
                    }
                    else {
                        execute(() -> {
                            handler.handleFileItemList(trashItems);
                        });
                    }
                }

                List<FileItem> resultList = new ArrayList<>();

                int beginIndex = trashItems.size();
                int endIndex = beginIndex + 19;
                List<FileItem> result = refreshTrashFiles(this.self.id, beginIndex, endIndex);
                while (!result.isEmpty()) {
                    // 添加结果
                    resultList.addAll(result);
                    if (result.size() < (endIndex - beginIndex + 1)) {
                        // 数量少于目标数量，结束
                        break;
                    }
                    beginIndex = endIndex + 1;
                    endIndex = beginIndex + 19;
                    result = refreshTrashFiles(this.self.id, beginIndex, endIndex);
                }

                if (trashItems.isEmpty()) {
                    synchronized (trashItems) {
                        // 更新内存
                        trashItems.addAll(resultList);

                        // 排序
                        sortFileItemsBySortableTime(trashItems);
                    }

                    if (handler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            handler.handleFileItemList(trashItems);
                        });
                    }
                    else {
                        execute(() -> {
                            handler.handleFileItemList(trashItems);
                        });
                    }
                }
                else {
                    synchronized (trashItems) {
                        // 更新内存
                        trashItems.addAll(resultList);

                        // 排序
                        sortFileItemsBySortableTime(trashItems);
                    }
                }
            });
        }
        else {
            if (handler.isInMainThread()) {
                executeOnMainThread(() -> {
                    handler.handleFileItemList(trashItemList);
                });
            }
            else {
                execute(() -> {
                    handler.handleFileItemList(trashItemList);
                });
            }
        }
    }

    private void sortFileItemsBySortableTime(List<FileItem> list) {
        Collections.sort(list, new Comparator<FileItem>() {
            @Override
            public int compare(FileItem fileItem1, FileItem fileItem2) {
                return (int) (fileItem2.getSortableTime() - fileItem1.getSortableTime());
            }
        });
    }

    private void removeTrashCache(Trash trash) {
        LinkedList<FileItem> trashItems = this.trashItemMap.get(trash.getRootId());
        if (null != trashItems) {
            synchronized (trashItems) {
                for (int i = 0; i < trashItems.size(); ++i) {
                    FileItem item = trashItems.get(i);

                    if (item.type == FileItem.ItemType.TrashDirectory &&
                        trash instanceof TrashDirectory) {
                        if (item.getTrashDirectory().id.longValue() == trash.id.longValue()) {
                            trashItems.remove(i);
                            break;
                        }
                    }
                    else if (item.type == FileItem.ItemType.TrashFile &&
                        trash instanceof TrashFile) {
                        if (item.getTrashFile().id.longValue() == trash.id.longValue()) {
                            trashItems.remove(i);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void addTrashCache(Long rootId, FileItem item) {
        LinkedList<FileItem> trashItems = this.trashItemMap.get(rootId);
        if (null != trashItems) {
            synchronized (trashItems) {
                trashItems.addFirst(item);
            }
        }
    }

    /**
     * 抹除指定的废弃数据。
     *
     * @param trash 指定废弃的数据。
     * @param successHandler
     * @param failureHandler
     */
    public void eraseTrash(Trash trash, TrashHandler successHandler, FailureHandler failureHandler) {
        if (!this.pipeline.isReady()) {
            // 移除缓存
            this.removeTrashCache(trash);
            // 从数据库里删除
            this.storage.deleteTrash(trash);

            ModuleError error = new ModuleError(FileStorage.NAME, FileStorageState.PipelineNotReady.code);
            execute(failureHandler, error);
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("root", trash.getRootId());

            JSONArray array = new JSONArray();
            array.put(trash.id.longValue());
            payload.put("list", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(FileStorageAction.EraseTrash, payload);
        this.pipeline.send(FileStorage.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(FileStorage.NAME, packet.state.code);
                    execute(failureHandler, error);
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != FileStorageState.Ok.code) {
                    ModuleError error = new ModuleError(FileStorage.NAME, stateCode);
                    execute(failureHandler, error);
                    return;
                }

                // 移除缓存
                removeTrashCache(trash);
                // 从数据库里删除
                storage.deleteTrash(trash);

                if (successHandler.isInMainThread()) {
                    executeOnMainThread(() -> {
                        successHandler.handleTrash(trash);
                    });
                }
                else {
                    execute(() -> {
                        successHandler.handleTrash(trash);
                    });
                }
            }
        });
    }

    /**
     * 恢复废弃的数据。
     *
     * @param trash 指定待恢复数据。
     * @param successHandler
     * @param failureHandler
     */
    public void restoreTrash(Trash trash, TrashHandler successHandler, FailureHandler failureHandler) {
        if (!this.pipeline.isReady()) {
            ModuleError error = new ModuleError(FileStorage.NAME, FileStorageState.PipelineNotReady.code);
            this.execute(failureHandler, error);
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("root", this.getSelfRoot().id.longValue());

            JSONArray array = new JSONArray();
            array.put(trash.getId().longValue());
            payload.put("list", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(FileStorageAction.RestoreTrash, payload);
        this.pipeline.send(FileStorage.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(FileStorage.NAME, packet.state.code);
                    execute(failureHandler, error);
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != FileStorageState.Ok.code) {
                    ModuleError error = new ModuleError(FileStorage.NAME, stateCode);
                    execute(failureHandler, error);
                    return;
                }

                JSONObject data = packet.extractServiceData();
                try {
                    JSONArray successList = data.getJSONArray("successList");
                    JSONArray failureList = data.getJSONArray("failureList");

                    Long rootId = trash.getRootId();

                    for (int i = 0; i < successList.length(); ++i) {
                        JSONObject trashJSON = successList.getJSONObject(i);
                        if (trashJSON.has("directory")) {
                            TrashDirectory trashDirectory = new TrashDirectory(trashJSON);
                            trashDirectory.setRootId(rootId);
                            // 恢复数据
                            restoreTrash(trashDirectory);
                        }
                        else if (trashJSON.has("file")) {
                            TrashFile trashFile = new TrashFile(trashJSON);
                            trashFile.setRootId(rootId);
                            // 恢复数据
                            restoreTrash(trashFile);
                        }
                    }

                    // 移除缓存
                    removeTrashCache(trash);
                    // 从数据库里删除
                    storage.deleteTrash(trash);

                    if (successHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            successHandler.handleTrash(trash);
                        });
                    }
                    else {
                        execute(() -> {
                            successHandler.handleTrash(trash);
                        });
                    }
                } catch (JSONException e) {
                    LogUtils.w(TAG, "#restoreTrash", e);
                }
            }
        });
    }

    private void restoreTrash(Trash trash) {
        if (trash instanceof TrashFile) {
            TrashFile trashFile = (TrashFile) trash;
            Directory parent = trashFile.getParent();

            FileHierarchy hierarchy = this.fileHierarchyMap.get(trashFile.getRootId());
            if (null != hierarchy) {
                if (hierarchy.restoreFileLabel(parent.id, trashFile.getFileLabel())) {
                    this.storage.writeFileLabel(parent, trashFile.getFileLabel());
                }
            }
        }
        else if (trash instanceof TrashDirectory) {
            TrashDirectory trashDirectory = (TrashDirectory) trash;
            FileHierarchy hierarchy = this.fileHierarchyMap.get(trashDirectory.getRootId());
            if (null != hierarchy) {
                hierarchy.restoreDirectory(trashDirectory.getDirectory());
            }
        }
    }

    private List<FileItem> refreshTrashFiles(Long rootId, int beginIndex, int endIndex) {
        List<FileItem> result = new ArrayList<>();

        if (!this.pipeline.isReady()) {
            return result;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("root", rootId.longValue());
            payload.put("begin", beginIndex);
            payload.put("end", endIndex);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet requestPacket = new Packet(FileStorageAction.ListTrash, payload);
        this.pipeline.send(FileStorage.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    synchronized (result) {
                        result.notify();
                    }
                    return;
                }

                if (packet.extractServiceStateCode() != FileStorageState.Ok.code) {
                    synchronized (result) {
                        result.notify();
                    }
                    return;
                }

                try {
                    JSONArray array = packet.extractServiceData().getJSONArray("list");
                    for (int i = 0; i < array.length(); ++i) {
                        JSONObject trashJSON = array.getJSONObject(i);
                        if (trashJSON.has("directory")) {
                            TrashDirectory trashDirectory = new TrashDirectory(trashJSON);
                            trashDirectory.setRootId(rootId);
                            FileItem item = new FileItem(trashDirectory);
                            result.add(item);

                            storage.writeTrashDirectory(trashDirectory);
                        }
                        else if (trashJSON.has("file")) {
                            TrashFile trashFile = new TrashFile(trashJSON);
                            trashFile.setRootId(rootId);
                            FileItem item = new FileItem(trashFile);
                            result.add(item);

                            storage.writeTrashFile(trashFile);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                synchronized (result) {
                    result.notify();
                }
            }
        });

        synchronized (result) {
            try {
                result.wait(this.blockingTimeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    /**
     * 上传到默认目录。
     *
     * @param fileAnchor
     * @param handler
     */
    public void uploadFile(FileAnchor fileAnchor, UploadFileHandler handler) {
        if (!this.hasStarted() || null == this.self) {
            if (handler.isInMainThread()) {
                this.executeOnMainThread(() -> {
                    ModuleError error = new ModuleError(NAME, FileStorageState.NotReady.code);
                    handler.handleFailure(error, fileAnchor);
                });
            }
            else {
                this.execute(() -> {
                    ModuleError error = new ModuleError(NAME, FileStorageState.NotReady.code);
                    handler.handleFailure(error, fileAnchor);
                });
            }
            return;
        }

        if (!fileAnchor.getFile().exists() || fileAnchor.getFile().length() == 0) {
            if (handler.isInMainThread()) {
                this.executeOnMainThread(() -> {
                    ModuleError error = new ModuleError(NAME, FileStorageState.ReadFileFailed.code);
                    handler.handleFailure(error, fileAnchor);
                });
            }
            else {
                this.execute(() -> {
                    ModuleError error = new ModuleError(NAME, FileStorageState.ReadFileFailed.code);
                    handler.handleFailure(error, fileAnchor);
                });
            }
            return;
        }

        try {
            fileAnchor.setUploadFileHandler(handler);
            fileAnchor.bindInputStream(new FileInputStream(fileAnchor.getFile()));

            // 将文件锚点添加到上传队列
            this.uploadQueue.enqueue(fileAnchor);
        } catch (IOException e) {
            LogUtils.w(this.getClass().getSimpleName(), e);
        }
    }

    public boolean isDownloading(String fileCode) {
        return this.downloadQueue.isProcessing(fileCode);
    }

    /**
     * 下载文件到默认的本地目录。
     *
     * @param fileLabel
     * @param handler
     */
    public void downloadFile(FileLabel fileLabel, DownloadFileHandler handler) {
        String filePath = fileLabel.getFilePath();
        if (null != filePath) {
            File file = new File(filePath);
            if (file.exists() && file.length() > 0) {
                if (LogUtils.isDebugLevel()) {
                    LogUtils.d(TAG, "File exists : " + fileLabel.getFileCode() + " -> " + file.getPath());
                }

                // 文件在本地已存在
                FileAnchor fileAnchor = new FileAnchor(file, fileLabel);
                fileAnchor.fileLabel.setFilePath(fileAnchor.getFilePath());
                if (handler.isInMainThread()) {
                    this.executeOnMainThread(() -> {
                        handler.handleSuccess(fileAnchor, fileLabel);
                    });
                }
                else {
                    this.execute(() -> {
                        handler.handleSuccess(fileAnchor, fileLabel);
                    });
                }

                return;
            }
        }

        // 判断文件码
        File localFile = new File(this.fileCachePath, fileLabel.getFileCode() + "." + fileLabel.getFileType());
        if (localFile.exists() && localFile.length() > 0) {
            if (LogUtils.isDebugLevel()) {
                LogUtils.d(TAG, "File exists : " + fileLabel.getFileCode() + " -> " + localFile.getPath());
            }

            // 文件在本地已存在
            FileAnchor fileAnchor = new FileAnchor(localFile, fileLabel);
            fileAnchor.fileLabel.setFilePath(fileAnchor.getFilePath());
            if (handler.isInMainThread()) {
                this.executeOnMainThread(() -> {
                    handler.handleSuccess(fileAnchor, fileLabel);
                });
            }
            else {
                this.execute(() -> {
                    handler.handleSuccess(fileAnchor, fileLabel);
                });
            }

            return;
        }

        synchronized (this) {
            FileAnchor current = this.downloadQueue.getProcessing(fileLabel.getFileCode());
            if (null != current) {
                LogUtils.d(TAG, "#downloadFile file is processing : " + current.getFileName());
                return;
            }

            // 下载文件
            FileAnchor anchor = new FileAnchor(localFile, fileLabel);

            LogUtils.d(TAG, "#downloadFile : " + anchor.getFileURL());
            anchor.setDownloadFileHandler(handler);
            this.downloadQueue.enqueue(anchor);
        }
    }

    /**
     * 上传文件数据到默认目录。
     *
     * @param filename
     * @param inputStream
     * @param handler
     * @deprecated 仅用于测试
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
                    execute(() -> {
                        handler.handleSuccess(fileLabel);
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
        PostTask postTask = (anchor, label) -> {
            // 设置文件路径
            label.setFilePath(anchor.getFilePath());

            // 数据入库
            this.storage.writeFileLabel(label);
        };

        // 上传完成之后获取文件标签
        this.execute(() -> {
            final UploadFileHandler uploadHandler = fileAnchor.getUploadFileHandler();

            // 获取文件标签
            getRemoteFileLabel(fileAnchor.getFileCode(), new StableFileLabelHandler() {
                @Override
                public void handleSuccess(FileLabel fileLabel) {
                    // 后处理
                    postTask.process(fileAnchor, fileLabel);

                    if (null != uploadHandler) {
                        if (uploadHandler.isInMainThread()) {
                            executeOnMainThread(() -> {
                                uploadHandler.handleSuccess(fileAnchor, fileLabel);
                            });
                        }
                        else {
                            execute(() -> {
                                uploadHandler.handleSuccess(fileAnchor, fileLabel);
                            });
                        }
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

                                    if (null != uploadHandler) {
                                        if (uploadHandler.isInMainThread()) {
                                            executeOnMainThread(() -> {
                                                uploadHandler.handleSuccess(fileAnchor, fileLabel);
                                            });
                                        }
                                        else {
                                            execute(() -> {
                                                uploadHandler.handleSuccess(fileAnchor, fileLabel);
                                            });
                                        }
                                    }
                                }

                                @Override
                                public void handleFailure(ModuleError error, @Nullable FileLabel fileLabel) {
                                    if (null != uploadHandler) {
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
                                }
                            });
                        }
                    }, 500);
                }
            });
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
    public void onDownloadStarted(FileAnchor fileAnchor) {
        DownloadFileHandler downloadHandler = fileAnchor.getDownloadHandler();
        if (null != downloadHandler) {
            if (downloadHandler.isInMainThread()) {
                this.executeOnMainThread(() -> {
                    downloadHandler.handleStarted(fileAnchor);
                });
            }
            else {
                this.execute(() -> {
                    downloadHandler.handleStarted(fileAnchor);
                });
            }
        }
    }

    @Override
    public void onDownloading(FileAnchor fileAnchor) {
        DownloadFileHandler downloadHandler = fileAnchor.getDownloadHandler();
        if (null != downloadHandler) {
            if (downloadHandler.isInMainThread()) {
                this.executeOnMainThread(() -> {
                    downloadHandler.handleProcessing(fileAnchor);
                });
            }
            else {
                this.execute(() -> {
                    downloadHandler.handleProcessing(fileAnchor);
                });
            }
        }
    }

    @Override
    public void onDownloadCompleted(FileAnchor fileAnchor) {
        // 设置标签的本地路径
        fileAnchor.fileLabel.setFilePath(fileAnchor.getFilePath());

        DownloadFileHandler downloadHandler = fileAnchor.getDownloadHandler();
        if (null != downloadHandler) {
            if (downloadHandler.isInMainThread()) {
                this.executeOnMainThread(() -> {
                    downloadHandler.handleSuccess(fileAnchor, fileAnchor.fileLabel);
                });
            }
            else {
                this.execute(() -> {
                    downloadHandler.handleSuccess(fileAnchor, fileAnchor.fileLabel);
                });
            }
        }
    }

    @Override
    public void onDownloadFailed(FileAnchor fileAnchor, int errorCode) {
        DownloadFileHandler downloadHandler = fileAnchor.getDownloadHandler();
        if (null != downloadHandler) {
            if (downloadHandler.isInMainThread()) {
                this.executeOnMainThread(() -> {
                    ModuleError error = new ModuleError(NAME, errorCode);
                    downloadHandler.handleFailure(error, fileAnchor);
                });
            }
            else {
                this.execute(() -> {
                    ModuleError error = new ModuleError(NAME, errorCode);
                    downloadHandler.handleFailure(error, fileAnchor);
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

    @Override
    public void notifyObservers(ObservableEvent event) {
        super.notifyObservers(event);

        if (FileStorageEvent.NewDirectory.equals(event.name)) {
            executeOnMainThread(() -> {
                for (DirectoryListener listener : directoryListeners) {
                    listener.onNewDirectory((Directory) event.getData(), (Directory) event.getSecondaryData());
                }
            });
        }
        else if (FileStorageEvent.DeleteDirectory.equals(event.name)) {
            Directory workingDirectory = (Directory) event.getData();
            // 将被删除的目录存入回收站
            TrashDirectory trashDirectory = new TrashDirectory(workingDirectory.getRoot().id,
                    (Directory) event.getSecondaryData());
            this.storage.writeTrashDirectory(trashDirectory);

            // 添加到缓存
            this.addTrashCache(workingDirectory.getRoot().id, new FileItem(trashDirectory));

            executeOnMainThread(() -> {
                for (DirectoryListener listener : directoryListeners) {
                    listener.onDeleteDirectory(workingDirectory, (Directory) event.getSecondaryData());
                }
            });
        }
        else if (FileStorageEvent.RenameDirectory.equals(event.name)) {
            executeOnMainThread(() -> {
                for (DirectoryListener listener : directoryListeners) {
                    listener.onRenameDirectory((Directory) event.getData());
                }
            });
        }
        else if (FileStorageEvent.DeleteFile.equals(event.name)) {
            // 将被删除的文件存入回收站
            TrashFile trashFile = new TrashFile((Directory) event.getData(), (FileLabel) event.getSecondaryData());
            this.storage.writeTrashFile(trashFile);

            // 添加到缓存
            this.addTrashCache(trashFile.getParent().getRoot().id, new FileItem(trashFile));
        }
    }

    interface PostTask {
        void process(FileAnchor fileAnchor, FileLabel fileLabel);
    }
}
