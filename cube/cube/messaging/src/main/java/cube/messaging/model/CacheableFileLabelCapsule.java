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

package cube.messaging.model;

import java.util.concurrent.ConcurrentLinkedQueue;

import cube.core.handler.FailureHandler;
import cube.core.model.Entity;
import cube.filestorage.model.FileLabel;
import cube.messaging.handler.LoadAttachmentHandler;

/**
 * 可缓存的附件列表。
 */
public class CacheableFileLabelCapsule extends Entity {

    public final ConcurrentLinkedQueue<Capsule> capsuleList;

    public CacheableFileLabelCapsule() {
        super();
        this.capsuleList = new ConcurrentLinkedQueue<>();
    }

    public synchronized void addMessage(Message message, FileLabel fileLabel) {
        this.capsuleList.add(new Capsule(message, fileLabel));
    }

    public synchronized void addMessage(Message message, FileLabel fileLabel, LoadAttachmentHandler loadHandler, FailureHandler failureHandler) {
        this.capsuleList.add(new Capsule(message, fileLabel, loadHandler, failureHandler));
    }

    public class Capsule {

        public final Message message;
        public final FileLabel fileLabel;
        public final LoadAttachmentHandler loadHandler;
        public final FailureHandler failureHandler;

        public Capsule(Message message, FileLabel fileLabel) {
            this.message = message;
            this.fileLabel = fileLabel;
            this.loadHandler = null;
            this.failureHandler = null;
        }

        public Capsule(Message message, FileLabel fileLabel, LoadAttachmentHandler loadHandler, FailureHandler failureHandler) {
            this.message = message;
            this.fileLabel = fileLabel;
            this.loadHandler = loadHandler;
            this.failureHandler = failureHandler;
        }
    }
}
