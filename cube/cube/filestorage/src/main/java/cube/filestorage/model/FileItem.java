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

/**
 * 文件项。
 */
public class FileItem {

    /**
     * 项目类型。
     */
    public enum ItemType {

        /**
         * 文件目录。
         */
        Directory,

        /**
         * 文件。
         */
        File
    }

    /**
     * 类型。
     */
    public final ItemType type;

    /**
     * 目录实例。
     */
    public final Directory directory;

    /**
     * 文件实例。
     */
    public final FileLabel fileLabel;

    public FileItem(Directory directory) {
        this.type = ItemType.Directory;
        this.directory = directory;
        this.fileLabel = null;
    }

    public FileItem(FileLabel fileLabel) {
        this.type = ItemType.File;
        this.fileLabel = fileLabel;
        this.directory = null;
    }

    public String getName() {
        if (this.type == ItemType.Directory) {
            return this.directory.getName();
        }
        else if (this.type == ItemType.File) {
            return this.fileLabel.getFileName();
        }
        else {
            return "";
        }
    }

    public long getLastModified() {
        if (this.type == ItemType.Directory) {
            return this.directory.getLastModified();
        }
        else if (this.type == ItemType.File) {
            return this.fileLabel.getLastModified();
        }
        else {
            return 0;
        }
    }
}
