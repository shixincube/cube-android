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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import cube.auth.AuthService;
import cube.core.model.Entity;

/**
 * 目录结构。
 */
public class Directory extends Entity {

    /**
     * 目录对应的层级描述。
     */
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
    private Map<String, FileLabel> files;

    public Directory(Long id, String name, long creation, long lastModified, long size, boolean hidden, int numDirs, int numFiles,
                     Long parentId) {
        super(id, creation);
        this.name = name;
        this.creation = creation;
        this.lastModified = lastModified;
        this.size = size;
        this.hidden = hidden;
        this.numDirs = numDirs;
        this.numFiles = numFiles;
        this.parentId = parentId;
    }

    /**
     * 获取父目录。
     *
     * @return 返回父目录。
     */
    public Directory getParent() {
        return this.parent;
    }

    /**
     * 是否是根目录。
     *
     * @return 如果当前目录是根目录返回 {@code true} 。
     */
    public boolean isRoot() {
        return (null == this.parent);
    }

    public JSONObject toJSON() {
        JSONObject json = super.toCompactJSON();
        try {
            json.put("domain", AuthService.getDomain());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
