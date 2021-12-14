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

package cube.multipointcomm.model;

import cube.contact.model.Self;
import cube.core.ModuleError;
import cube.multipointcomm.util.MediaConstraint;

/**
 * 通话记录。
 */
public class CallRecord {

    private Self self;

    public final CommField field;

    private long startTime;

    private long answerTime;

    private long endTime;

    private MediaConstraint callerConstraint;

    private MediaConstraint calleeConstraint;

    public ModuleError lastError;

    public CallRecord(Self self, CommField commField) {
        this.self = self;
        this.field = commField;
        this.startTime = System.currentTimeMillis();
    }

    public boolean isActive() {
        return (null != this.field && this.field.numRTCDevices() > 0);
    }

    public long getStartTime() {
        return this.startTime;
    }

    public void setStartTime(long time) {
        this.startTime = time;
    }

    public long getAnswerTime() {
        return this.answerTime;
    }

    public void setAnswerTime(long time) {
        this.answerTime = time;
    }

    public long getEndTime() {
        return this.endTime;
    }

    public void setEndTime(long time) {
        this.endTime = time;
    }

    public void setCallerConstraint(MediaConstraint mediaConstraint) {
        this.callerConstraint = mediaConstraint;
    }

    public MediaConstraint getCallerConstraint() {
        return this.callerConstraint;
    }

    public void setCalleeConstraint(MediaConstraint mediaConstraint) {
        this.calleeConstraint = mediaConstraint;
    }

    public MediaConstraint getCalleeConstraint() {
        return this.calleeConstraint;
    }
}
