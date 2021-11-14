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

package cube.messaging.handler;

import cube.core.handler.CallbackHandler;
import cube.filestorage.model.FileAnchor;
import cube.messaging.model.FileAttachment;
import cube.messaging.model.Message;

/**
 * 加载附件句柄。
 */
public interface LoadAttachmentHandler<MessageInstanceType extends Message> extends CallbackHandler {

    /**
     * 当附件正在加载时回调。
     *
     * @param message
     */
    void handleLoading(MessageInstanceType message, FileAttachment fileAttachment, FileAnchor fileAnchor);

    /**
     * 附件加载后回调。
     *
     * @param message
     */
    void handleLoaded(MessageInstanceType message, FileAttachment fileAttachment);
}
