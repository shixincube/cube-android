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

package cube.filestorage.handler;

import androidx.annotation.Nullable;

import cube.core.ModuleError;
import cube.core.handler.CallbackHandler;
import cube.filestorage.model.FileAnchor;
import cube.filestorage.model.FileLabel;

/**
 * 上传文件句柄。
 */
public interface UploadFileHandler extends CallbackHandler {

    /**
     * 上传流程已启动。
     *
     * @param anchor
     */
    void handleStarted(FileAnchor anchor);

    /**
     * 正在进行文件处理的回调函数。
     *
     * @param anchor
     */
    void handleProcessing(FileAnchor anchor);

    /**
     * 上传文件成功的回调函数。
     *
     * @param fileAnchor
     * @param fileLabel
     */
    void handleSuccess(FileAnchor fileAnchor, FileLabel fileLabel);

    /**
     * 上传文件失败的回调函数。
     *
     * @param error
     * @param anchor
     */
    void handleFailure(ModuleError error, @Nullable FileAnchor anchor);
}
