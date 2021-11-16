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

import java.util.HashMap;
import java.util.Map;

import cube.filestorage.FileStorage;
import cube.filestorage.StructStorage;

/**
 * 文件层级结构描述。
 */
public class FileHierarchy {

    private final FileStorage service;

    private final StructStorage storage;

    /**
     * 根目录 ID 。
     */
    private Long rootId;

    /**
     * 当前层级的根目录。
     */
    private Directory root;

    /**
     * 当前层级包含的目录。
     */
    private Map<Long, Directory> directoryMap;

    public FileHierarchy(FileStorage service, StructStorage storage, Long rootId) {
        this.service = service;
        this.storage = storage;
        this.directoryMap = new HashMap<>();
    }

    public Directory getRoot() {
        if (null != this.root) {
            return this.root;
        }

        this.root = this.storage.readDirectory(this.rootId);

        return this.root;
    }
}
