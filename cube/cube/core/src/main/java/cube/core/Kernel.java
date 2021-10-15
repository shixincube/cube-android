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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cube.auth.AuthToken;
import cube.auth.handler.AuthTokenHandler;
import cube.core.handler.KernelHandler;
import cube.pipelline.CellPipeline;

/**
 * 内核。内核管理所有的模块和通信管道。
 */
public class Kernel implements PipelineListener {

    private final static int MAX_THREADS = 4;

    private Context context;

    private KernelConfig config;

    private boolean working;

    private Pipeline pipeline;

    private Map<String, Module> moduleMap;

    private ExecutorService executor;

    public Kernel() {
        this.working = false;
        this.moduleMap = new HashMap<>();
        this.executor = Executors.newFixedThreadPool(MAX_THREADS);
    }

    public boolean startup(Context context, KernelConfig config, KernelHandler handler) {
        if (null == config || null == handler) {
            return false;
        }

        this.pipeline = new CellPipeline(context);

        this.context = context;

        this.config = config;

        if (null == this.executor) {
            this.executor = Executors.newFixedThreadPool(MAX_THREADS);
        }

        // 处理模块
        this.bundle();

        // 启动管道
        this.pipeline.setRemoteAddress(config.address, config.port);
        this.pipeline.addListener(this);
//        this.pipeline.open();

        // 检测授权
        boolean ret = this.checkAuth(config, new AuthTokenHandler() {
            @Override
            public void handleSuccess(AuthToken authToken) {

            }

            @Override
            public void handleFailure(ModuleError error) {

            }
        });

        return ret;
    }

    public void shutdown() {
        if (null != this.pipeline) {
            this.pipeline.close();
            this.pipeline = null;
        }

        this.executor.shutdown();
        this.executor = null;
    }

    public void suspend() {

    }

    public void resume() {

    }

    public boolean isReady() {
        return false;
    }

    public AuthToken activeToken(Long contactId) {
        return null;
    }

    public void installModule(Module module) {
        module.kernel = this;

        this.moduleMap.put(module.name, module);
    }

    public void uninstallModule(Module module) {
        this.moduleMap.remove(module.name);
    }

    public Module getModule(String moduleName) {
        return this.moduleMap.get(moduleName);
    }

    public boolean hasModule(String moduleName) {
        return this.moduleMap.containsKey(moduleName);
    }

    protected Context getContext() {
        return this.context;
    }

    protected ExecutorService getExecutor() {
        return this.executor;
    }

    /**
     * 进行数据对象关联
     */
    private void bundle() {
        for (Module module : this.moduleMap.values()) {
            module.pipeline = this.pipeline;
        }
    }

    private boolean checkAuth(KernelConfig config, AuthTokenHandler handler) {
        return false;
    }

    @Override
    public void received(Pipeline pipeline, String source, Packet packet) {

    }

    @Override
    public void opened(Pipeline pipeline) {

    }

    @Override
    public void closed(Pipeline pipeline) {

    }

    @Override
    public void faultOccurred(Pipeline pipeline, int code, String description) {

    }
}
