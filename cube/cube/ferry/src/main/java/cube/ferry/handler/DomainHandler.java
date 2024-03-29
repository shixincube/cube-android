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

package cube.ferry.handler;

import java.util.List;

import cube.auth.model.AuthDomain;
import cube.core.handler.CallbackHandler;
import cube.ferry.model.DomainInfo;
import cube.ferry.model.DomainMember;

/**
 * 域信息句柄。
 */
public abstract class DomainHandler implements CallbackHandler {

    private final boolean inMainThread;

    public DomainHandler() {
        this.inMainThread = true;
    }

    public DomainHandler(boolean inMainThread) {
        this.inMainThread = inMainThread;
    }

    @Override
    public boolean isInMainThread() {
        return this.inMainThread;
    }

    /**
     * 处理域相关数据。
     *
     * @param authDomain
     * @param domainInfo
     * @param members
     */
    public abstract void handleDomain(AuthDomain authDomain, DomainInfo domainInfo, List<DomainMember> members);
}
