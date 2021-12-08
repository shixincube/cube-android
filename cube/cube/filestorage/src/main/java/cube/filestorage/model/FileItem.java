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

import cube.core.model.TimeSortable;

/**
 * 文件项。
 */
public class FileItem implements TimeSortable {

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
        File,

        /**
         * 父目录。
         */
        ParentDirectory,

        /**
         * 废弃目录。
         */
        TrashDirectory,

        /**
         * 废弃文件。
         */
        TrashFile
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

    /**
     * 废弃的目录实例。
     */
    public final TrashDirectory trashDirectory;

    /**
     * 废弃的文件实例。
     */
    public final TrashFile trashFile;

    public FileItem(Directory directory) {
        this.type = ItemType.Directory;
        this.directory = directory;
        this.fileLabel = null;
        this.trashDirectory = null;
        this.trashFile = null;
    }

    public FileItem(FileLabel fileLabel) {
        this.type = ItemType.File;
        this.fileLabel = fileLabel;
        this.directory = null;
        this.trashDirectory = null;
        this.trashFile = null;
    }

    public FileItem(TrashDirectory trashDirectory) {
        this.type = ItemType.TrashDirectory;
        this.trashDirectory = trashDirectory;
        this.trashFile = null;
        this.directory = null;
        this.fileLabel = null;
    }

    public FileItem(TrashFile trashFile) {
        this.type = ItemType.TrashFile;
        this.trashFile = trashFile;
        this.trashDirectory = null;
        this.directory = null;
        this.fileLabel = null;
    }

    protected FileItem(Directory directory, ItemType type) {
        this.type = type;
        this.directory = directory;
        this.fileLabel = null;
        this.trashDirectory = null;
        this.trashFile = null;
    }

    public ItemType getType() {
        return this.type;
    }

    public String getName() {
        if (this.type == ItemType.Directory) {
            return this.directory.getName();
        }
        else if (this.type == ItemType.File) {
            return this.fileLabel.getFileName();
        }
        else if (this.type == ItemType.TrashDirectory) {
            return this.trashDirectory.getDirectory().getName();
        }
        else if (this.type == ItemType.TrashFile) {
            return this.trashFile.getFileLabel().getFileName();
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
            return this.fileLabel.getCompletedTime();
        }
        else {
            return this.getSortableTime();
        }
    }

    public Directory getDirectory() {
        if (null != this.directory) {
            return this.directory;
        }
        else if (null != this.trashDirectory) {
            return this.trashDirectory.getDirectory();
        }

        return null;
    }

    public FileLabel getFileLabel() {
        if (null != this.fileLabel) {
            return this.fileLabel;
        }
        else if (null != this.trashFile) {
            return this.trashFile.getFileLabel();
        }

        return null;
    }

    public TrashDirectory getTrashDirectory() {
        return this.trashDirectory;
    }

    public TrashFile getTrashFile() {
        return this.trashFile;
    }

    @Override
    public long getSortableTime() {
        if (this.type == ItemType.Directory) {
            return this.directory.getLastModified();
        }
        else if (this.type == ItemType.File) {
            return this.fileLabel.getCompletedTime();
        }
        else if (this.type == ItemType.ParentDirectory) {
            return this.directory.getLastModified();
        }
        else if (this.type == ItemType.TrashDirectory) {
            return this.trashDirectory.getTimestamp();
        }
        else if (this.type == ItemType.TrashFile) {
            return this.trashFile.getTimestamp();
        }

        return 0;
    }

    /**
     * 创建父目录。
     *
     * @param parent
     * @return
     */
    public static FileItem createParentDirectory(Directory parent) {
        return new FileItem(parent, ItemType.ParentDirectory);
    }
}
