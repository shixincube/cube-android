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

/**
 * 文件存储服务动作定义。
 */
public final class FileStorageAction {

    /**
     * 文件放置到存储中。
     */
    public final static String PutFile = "putFile";

    /**
     * 获取文件的标签。
     */
    public final static String GetFile = "getFile";

    /**
     * 获取根文件夹。
     */
    public final static String GetRoot = "getRoot";

    /**
     * 插入文件到目录。
     */
    public final static String InsertFile = "insertFile";

    /**
     * 罗列目录清单。
     */
    public final static String ListDirs = "listDirs";

    /**
     * 罗列文件清单。
     */
    public final static String ListFiles = "listFiles";

    /**
     * 创建新目录。
     */
    public final static String NewDir = "newDir";

    /**
     * 删除目录。
     */
    public final static String DeleteDir = "deleteDir";

    /**
     * 重命名目录。
     */
    public final static String  RenameDir = "renameDir";

    /**
     * 删除文件。
     */
    public final static String DeleteFile = "deleteFile";

    /**
     * 罗列回收站里的废弃数据。
     */
    public final static String ListTrash = "listTrash";

    /**
     * 抹除回收站里的废弃数据。
     */
    public final static String EraseTrash = "eraseTrash";

    /**
     * 清空回收站里的废弃数据。
     */
    public final static String EmptyTrash = "emptyTrash";

    /**
     * 从回收站恢复废弃数据。
     */
    public final static String RestoreTrash = "restoreTrash";

    /**
     * 检索文件。
     */
    public final static String SearchFile = "searchFile";

    private FileStorageAction() {
    }
}
