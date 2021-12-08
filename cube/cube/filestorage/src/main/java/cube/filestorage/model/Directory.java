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

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import cube.auth.AuthService;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.FailureHandler;
import cube.core.handler.StableFailureHandler;
import cube.core.model.Entity;
import cube.filestorage.FileStorage;
import cube.filestorage.handler.DefaultDirectoryListHandler;
import cube.filestorage.handler.DefaultFileListHandler;
import cube.filestorage.handler.DirectoryFileDownloadHandler;
import cube.filestorage.handler.DirectoryHandler;
import cube.filestorage.handler.FileItemListHandler;
import cube.filestorage.handler.DirectoryFileUploadHandler;
import cube.util.LogUtils;

/**
 * 目录结构。
 */
public class Directory extends Entity implements Comparator<FileItem> {

    private final static Collator sCollator = Collator.getInstance(Locale.CHINESE);

    private final static String TAG = "Directory";
    
    private FileHierarchy hierarchy;

    /**
     * 父目录 ID 。
     */
    private Long parentId;

    /**
     * 父目录。
     */
    private Directory parent;

    /**
     * 目录名。
     */
    private String name;

    /**
     * 目录的创建时间。
     */
    private long creation;

    /**
     * 目录最后一次修改时间。
     */
    private long lastModified;

    /**
     * 是否是隐藏目录。
     */
    private boolean hidden = false;

    /**
     * 目录内所有各级文件大小总和。
     */
    private long size;

    /**
     * 当前目录的子目录数量。
     */
    private int numDirs;

    /**
     * 子目录映射。
     */
    private Map<Long, Directory> children;

    /**
     * 当前目录下的文件数量。
     */
    private int numFiles;

    /**
     * 包含的文件映射。
     */
    private List<FileLabel> files;

    public Directory(Long id, String name, long creation, long lastModified, long size, boolean hidden, int numDirs, int numFiles,
                     Long parentId, long lastTime, long expiryTime) {
        super(id, creation);
        this.name = name;
        this.creation = creation;
        this.lastModified = lastModified;
        this.size = size;
        this.hidden = hidden;
        this.numDirs = numDirs;
        this.numFiles = numFiles;
        this.parentId = parentId;
        this.last = lastTime;
        this.expiry = expiryTime;
        this.children = new ConcurrentHashMap<>();
        this.files = new ArrayList<>();
    }

    public Directory(JSONObject json) throws JSONException {
        super(json);
        this.name = json.getString("name");
        this.creation = json.getLong("creation");
        this.lastModified = json.getLong("lastModified");
        this.size = json.getLong("size");
        this.hidden = json.getBoolean("hidden");
        this.timestamp = this.lastModified;

        this.numDirs = json.getInt("numDirs");
        this.numFiles = json.getInt("numFiles");

        if (json.has("parentId")) {
            this.parentId = json.getLong("parentId");
        }
        else {
            this.parentId = 0L;
        }

        this.children = new ConcurrentHashMap<>();
        this.files = new ArrayList<>();

        if (json.has("parent")) {
            this.parent = new Directory(json.getJSONObject("parent"));
        }

        if (json.has("dirs")) {
            JSONArray array = json.getJSONArray("dirs");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject data = array.getJSONObject(i);
                Directory directory = new Directory(data);
                this.addChild(directory);
            }
        }

        if (json.has("files")) {
            JSONArray array = json.getJSONArray("files");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject data = array.getJSONObject(i);
                FileLabel fileLabel = new FileLabel(data);
                this.addFile(fileLabel);
            }
        }
    }

    /**
     * <b>Non-public API</b>
     * @param hierarchy
     */
    protected void setHierarchy(FileHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    protected Collection<Directory> getChildren() {
        return this.children.values();
    }

    protected int countChildren() {
        return this.children.size();
    }

    protected void addChild(Directory directory) {
        this.children.put(directory.id, directory);
        directory.setParent(this);
        directory.setHierarchy(this.hierarchy);
    }

    protected void removeChild(Directory directory) {
        this.children.remove(directory.id);
    }

    protected boolean addFile(FileLabel fileLabel) {
        if (!this.files.contains(fileLabel)) {
            this.files.add(fileLabel);
            return true;
        }
        else {
            return false;
        }
    }

    protected void removeFile(FileLabel fileLabel) {
        this.files.remove(fileLabel);
    }

    protected void setNumDirs(int num) {
        this.numDirs = num;
    }

    /**
     * 获取父目录。
     *
     * @return 返回父目录。
     */
    public Directory getParent() {
        return this.parent;
    }

    public Long getParentId() {
        return this.parentId;
    }

    protected void setParent(Directory parent) {
        if (parent == this || this.id.longValue() == parent.id.longValue()) {
            return;
        }

        this.parent = parent;
    }

    public boolean hasParent() {
        return (null != this.parent);
    }

    /**
     * 获取目录名。
     *
     * @return 返回目录名。
     */
    public String getName() {
        return this.name;
    }

    /**
     *
     * @return
     */
    public long getCreation() {
        return this.creation;
    }

    /**
     *
     * @return
     */
    public long getLastModified() {
        return this.lastModified;
    }

    /**
     *
     * @return
     */
    public long getSize() {
        return this.size;
    }

    /**
     *
     * @return
     */
    public boolean isHidden() {
        return this.hidden;
    }

    /**
     *
     * @return
     */
    public int numDirs() {
        return this.numDirs;
    }

    /**
     *
     * @return
     */
    public int numFiles() {
        return this.numFiles;
    }

    /**
     * 是否是根目录。
     *
     * @return 如果当前目录是根目录返回 {@code true} 。
     */
    public boolean isRoot() {
        return (this.parentId.longValue() == 0);
    }

    /**
     * 返回正在上传的文件数量。
     *
     * @return 返回正在上传的文件数量。
     */
    public int numUploadingFiles() {
        return this.hierarchy.getUploadingFiles().size();
    }

    /**
     * 返回正在下载的文件数量。
     *
     * @return 返回正在下载的文件数量。
     */
    public int numDownloadingFiles() {
        return this.hierarchy.getDownloadingFiles().size();
    }

    /**
     * 是否是空目录。
     *
     * @return 如果目录下面既没有子目录也没有文件返回 {@code true} 。
     */
    public boolean isEmpty() {
        return (this.numDirs == 0 && this.numFiles == 0);
    }

    /**
     * 列举当前目录下的所有子目录和文件。
     *
     * @param successHandler 指定操作成功回调句柄。
     * @param failureHandler 指定操作失败回调句柄。
     */
    public void listFileItems(FileItemListHandler successHandler, FailureHandler failureHandler) {
        final List<FileItem> result = new ArrayList<>();

        AtomicBoolean gotDirs = new AtomicBoolean(false);
        AtomicBoolean gotFiles = new AtomicBoolean(false);

        // 读取子目录
        if (this.numDirs == this.children.size()) {
            for (Directory directory : this.children.values()) {
                result.add(new FileItem(directory));
            }

            if (LogUtils.isDebugLevel()) {
                LogUtils.d(TAG, "#listFileItems - (" + this.name + ") same dir num: " + this.numDirs);
            }

            gotDirs.set(true);
            if (gotFiles.get()) {
                processList(result, successHandler);
            }
        }
        else {
            this.hierarchy.listDirectories(this, new DefaultDirectoryListHandler(false) {
                @Override
                public void handleDirectoryList(List<Directory> directoryList) {
                    for (Directory directory : directoryList) {
                        result.add(new FileItem(directory));
                    }

                    gotDirs.set(true);
                    if (gotFiles.get()) {
                        processList(result, successHandler);
                    }
                }
            }, new StableFailureHandler() {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    LogUtils.w(TAG, "#listDirectories failed : " + error.code);
                    gotDirs.set(true);
                    if (gotFiles.get()) {
                        hierarchy.handle(failureHandler, new ModuleError(FileStorage.NAME, error.code));
                    }
                }
            });
        }

        // 读取文件
        if (this.numFiles == this.files.size()) {
            for (FileLabel fileLabel : this.files) {
                result.add(new FileItem(fileLabel));
            }

            if (LogUtils.isDebugLevel()) {
                LogUtils.d(TAG, "#listFileItems - (" + this.name + ") same file num: " + this.numFiles);
            }

            gotFiles.set(true);
            if (gotDirs.get()) {
                processList(result, successHandler);
            }
        }
        else {
            int beginIndex = this.files.size();
            int endIndex = beginIndex + 19;
            this.hierarchy.listFiles(this, beginIndex, endIndex, new DefaultFileListHandler(false) {
                @Override
                public void handleFileList(List<FileLabel> fileList) {
                    for (FileLabel fileLabel : fileList) {
                        result.add(new FileItem(fileLabel));
                    }

                    gotFiles.set(true);
                    if (gotDirs.get()) {
                        processList(result, successHandler);
                    }
                }
            }, new StableFailureHandler() {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    LogUtils.w(TAG, "#listFiles failed : " + error.code);

                    gotFiles.set(true);
                    if (gotDirs.get()) {
                        processList(result, successHandler);
                    }
                }
            });
        }
    }

    private void processList(List<FileItem> itemList, FileItemListHandler handler) {
        // 分离目录和文件
        List<FileItem> fileList = new ArrayList<>();
        Iterator<FileItem> iter = itemList.iterator();
        while (iter.hasNext()) {
            FileItem item = iter.next();
            if (item.type == FileItem.ItemType.File) {
                fileList.add(item);
                iter.remove();
            }
        }

        // 排序
        Collections.sort(fileList, this);
        Collections.sort(itemList, this);

        itemList.addAll(fileList);

        this.hierarchy.handle(handler, itemList);
    }

    /**
     * 上传文件。
     *
     * @param file 指定文件。
     * @param successHandler 指定操作成功回调句柄。
     * @param failureHandler 指定操作失败回调句柄。
     */
    public void uploadFile(File file, DirectoryFileUploadHandler successHandler, FailureHandler failureHandler) {
        this.hierarchy.uploadFile(file, this, successHandler, failureHandler);
    }

    /**
     * 下载文件。
     *
     * @param fileLabel
     * @param outputFile
     * @param successHandler
     * @param failureHandler
     */
    public void downloadFile(FileLabel fileLabel, File outputFile, DirectoryFileDownloadHandler successHandler, FailureHandler failureHandler) {

    }

    /**
     * 删除指定文件。
     *
     * @param fileLabel 指定文件标签。
     * @param successHandler 指定操作成功回调句柄。
     * @param failureHandler 指定操作失败回调句柄。
     */
    public void deleteFile(FileLabel fileLabel, DirectoryHandler successHandler, FailureHandler failureHandler) {
        this.hierarchy.deleteFile(this, fileLabel, successHandler, failureHandler);
    }

    /**
     * 获取指定名称的子目录。
     *
     * @param dirName 指定子目录名。
     * @return 返回目录实例。
     */
    public Directory getSubdirectory(String dirName) {
        for (Directory directory : this.children.values()) {
            if (directory.getName().equals(dirName)) {
                return directory;
            }
        }

        return null;
    }

    /**
     * 新建文件夹。
     *
     * @param directoryName 指定新文件夹名。
     * @param successHandler 指定操作成功回调句柄。
     * @param failureHandler 指定操作失败回调句柄。
     */
    public void newDirectory(String directoryName, DirectoryHandler successHandler, FailureHandler failureHandler) {
        this.hierarchy.newDirectory(this, directoryName, successHandler, failureHandler);
    }

    /**
     * 删除指定的子目录。如果目录是非空目录则不会被删除。
     *
     * @param subdirectory 指定待删除的子目录。
     * @param recursive 指定是否递归删除指定目录下的所有目录和文件。
     * @param successHandler 指定操作成功回调句柄。
     * @param failureHandler 指定操作失败回调句柄。
     */
    public void deleteDirectory(Directory subdirectory, boolean recursive, DirectoryHandler successHandler, FailureHandler failureHandler) {
        this.hierarchy.deleteDirectory(this, subdirectory, recursive, successHandler, failureHandler);
    }

    /**
     * 重命名文件夹。
     *
     * @param newName 指定目录的新名称。
     * @param successHandler 指定操作成功回调句柄。
     * @param failureHandler 指定操作失败回调句柄。
     */
    public void renameDirectory(String newName, DirectoryHandler successHandler, FailureHandler failureHandler) {
        this.hierarchy.renameDirectory(this, newName, successHandler, failureHandler);
    }

    protected void update(Directory source) {
        this.numFiles = source.numFiles;
        this.numDirs = source.numDirs;
        this.name = source.name;
        this.lastModified = source.lastModified;
        this.size = source.size;
        this.hidden = source.hidden;
        this.resetLast(System.currentTimeMillis());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = this.toCompactJSON();
        try {
            json.put("domain", AuthService.getDomain());

            if (null != this.parent) {
                json.put("parent", this.parent.toCompactJSON());
            }

            if (!this.children.isEmpty()) {
                JSONArray childrenArray = new JSONArray();
                for (Directory directory : this.children.values()) {
                    childrenArray.put(directory.toJSON());
                }
                json.put("dirs", childrenArray);
            }

            if (!this.files.isEmpty()) {
                JSONArray fileArray = new JSONArray();
                for (FileLabel fileLabel : this.files) {
                    fileArray.put(fileLabel.toJSON());
                }
                json.put("files", fileArray);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        try {
            json.put("name", this.name);
            json.put("creation", this.creation);
            json.put("lastModified", this.lastModified);
            json.put("size", this.size);
            json.put("hidden", this.hidden);
            json.put("numDirs", this.numDirs);
            json.put("numFiles", this.numFiles);
            json.put("parentId", this.parentId.longValue());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public int compare(FileItem fileItem1, FileItem fileItem2) {
        return sCollator.compare(fileItem1.getName(), fileItem2.getName());
    }
}
