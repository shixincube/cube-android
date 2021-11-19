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

package cube.contact;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cube.contact.handler.GroupAppendixHandler;
import cube.contact.model.Group;
import cube.contact.model.GroupAppendix;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.CompletionHandler;
import cube.core.handler.FailureHandler;

/**
 * 工作中的群组列表句柄。
 */
public class WorkingGroupListHandler implements GroupAppendixHandler, FailureHandler {

    private final ContactService service;

    private final CompletionHandler completionHandler;

    private int total = 0;

    private List<Group> groupList;

    private AtomicInteger appendixCount;

    public WorkingGroupListHandler(ContactService service, CompletionHandler completionHandler) {
        this.service = service;
        this.completionHandler = completionHandler;
        this.groupList = new ArrayList<>();
        this.appendixCount = new AtomicInteger(0);
    }

    public void setTotal(int total) {
        if (total > 0) {
            this.total = total;
        }
    }

    public void firePageLoaded() {
        if (this.total == 0) {
            this.finish();
        }
    }

    public void addGroup(Group group) {
        this.groupList.add(group);

        // 写入缓存
        this.service.cache.put(group.id, group);
    }

    private void finish() {
        this.completionHandler.handleCompletion(this.service);
        this.service.workingGroupListHandler = null;
    }

    @Override
    public void handleAppendix(Group group, GroupAppendix appendix) {
        if (this.appendixCount.incrementAndGet() == this.total) {
            finish();
        }
    }

    @Override
    public void handleFailure(Module module, ModuleError error) {
        if (this.appendixCount.incrementAndGet() == this.total) {
            finish();
        }
    }

    @Override
    public boolean isInMainThread() {
        return false;
    }
}
