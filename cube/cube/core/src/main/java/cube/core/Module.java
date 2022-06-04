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

package cube.core;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import cell.util.NetworkUtils;
import cube.auth.AuthToken;
import cube.core.handler.FailureHandler;
import cube.util.LogUtils;
import cube.util.Subject;

/**
 * 内核模块。
 */
public abstract class Module extends Subject {

    /**
     * 内存寿命。
     */
    protected final static long LIFESPAN = 5L * 60L * 1000L;

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

    protected final PluginSystem pluginSystem;

    /**
     * 主线程句柄。
     */
    private Handler mainHandler;

    /**
     * 任务队列。
     */
    private Queue<Runnable> taskQueue;

    /**
     * 是否已启动。
     */
    private boolean started;

    public Module(String name) {
        this.name = name;
        this.started = false;
        this.pluginSystem = new PluginSystem();
        this.taskQueue = new ConcurrentLinkedQueue<>();
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

        this.mainHandler = new Handler(this.kernel.looper);

        this.started = true;
        return true;
    }

    /**
     * 停止模块。
     */
    public void stop() {
        this.started = false;

        this.taskQueue.clear();
    }

    public void suspend() {
        // subclass hook override.
    }

    public void resume() {
        // subclass hook override.
    }

    /**
     * 引擎启动后对模块进行配置。
     *
     * @param configData
     */
    protected abstract void config(@Nullable JSONObject configData);

    public Kernel getKernel() {
        return this.kernel;
    }

    protected Pipeline getPipeline() {
        return this.pipeline;
    }

    protected Context getContext() {
        return this.kernel.getContext();
    }

    protected AuthToken getAuthToken() {
        return this.kernel.getAuthToken();
    }

    public void execute(Runnable task) {
        this.taskQueue.offer(task);

        this.kernel.getExecutor().execute(() -> {
            Runnable current = taskQueue.poll();
            while (null != current) {

                current.run();

                current = taskQueue.poll();
            }
        });
    }

    protected void executeDelayed(Runnable task, long delayMillis) {
        if (delayMillis > 1000L) {
            LogUtils.w(Module.class.getSimpleName(), "Not recommended delay millis more than 1000 millis");
        }

        this.kernel.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                execute(task);
            }
        });
    }

    public void execute(FailureHandler failureHandler, ModuleError error) {
        if (failureHandler.isInMainThread()) {
            executeOnMainThread(() -> {
                failureHandler.handleFailure(this, error);
            });
        }
        else {
            execute(() -> {
                failureHandler.handleFailure(this, error);
            });
        }
    }

    protected void executeOnMainThread(Runnable task) {
        this.mainHandler.post(task);
    }

    protected boolean isAvailableNetwork() {
        return NetworkUtils.isAvailable(this.kernel.getContext());
    }

    public abstract boolean isReady();
}
