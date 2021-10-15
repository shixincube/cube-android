/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Shixin Cube Team.
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

package cube.core;

import android.content.Context;

import cube.util.Subject;

/**
 * 内核模块。
 */
public abstract class Module extends Subject {

    /**
     * 模块名。
     */
    public final String name;

    /**
     * 全局内核。
     */
    protected Kernel kernel;

    /**
     * 数据管道。
     */
    protected Pipeline pipeline;

    private boolean started;

    public Module(String name) {
        this.name = name;
        this.started = false;
    }

    public final String getName() {
        return this.name;
    }

    /**
     * 模块是否已启动。
     *
     * @return
     */
    public boolean hasStarted() {
        return this.started;
    }

    /**
     * 启动模块。
     *
     * @return
     */
    public boolean start() {
        if (this.started) {
            return false;
        }

        this.started = true;
        return true;
    }

    /**
     * 停止模块。
     */
    public void stop() {
        this.started = false;
    }

    public void suspend() {
        // subclass hook override.
    }

    public void resume() {
        // subclass hook override.
    }

    protected Context getContext() {
        return this.kernel.getContext();
    }

    protected void execute(Runnable task) {
        this.kernel.getExecutor().execute(task);
    }

    public abstract boolean isReady();
}
