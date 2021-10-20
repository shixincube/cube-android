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

package cube.engine;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import cube.auth.AuthService;
import cube.contact.ContactService;
import cube.contact.handler.SignHandler;
import cube.contact.model.Self;
import cube.core.Kernel;
import cube.core.KernelConfig;
import cube.core.ModuleError;
import cube.core.handler.KernelHandler;
import cube.engine.handler.EngineHandler;

/**
 * 魔方引擎 API 入口类。
 */
public class CubeEngine {

    protected static CubeEngine instance = null;

    private KernelConfig config;

    private Kernel kernel;

    private boolean started;

    private CubeEngine() {
        this.started = false;
        this.kernel = new Kernel();
        this.kernel.installModule(new AuthService());
        this.kernel.installModule(new ContactService());
    }

    public static CubeEngine getInstance() {
        if (null == CubeEngine.instance) {
            CubeEngine.instance = new CubeEngine();
        }
        return CubeEngine.instance;
    }

    public void setConfig(KernelConfig config) {
        this.config = config;
    }

    public KernelConfig getConfig() {
        return this.config;
    }

    /**
     * 启动引擎。
     *
     * @param context 应用上下文。
     * @param handler 处理句柄。
     * @return
     */
    public boolean start(Context context, EngineHandler handler) {
        Log.i("CubeEngine", "#start : " + this.config.print());

        this.started = this.kernel.startup(context, this.config, new KernelHandler() {
            @Override
            public void handleCompletion(Kernel kernel) {
                Log.i("CubeEngine", "Cube engine started");

                // 启动联系人模块
                getContactService().start();

                handler.handleSuccess(CubeEngine.instance);
            }

            @Override
            public void handleFailure(ModuleError error) {
                // 启动失败
                started = false;

                Log.i("CubeEngine", "Cube engine start failed : " + error.code);
                handler.handleFailure(error.code, (null != error.description) ? error.description : error.moduleName);
            }
        });

        return this.started;
    }

    public void stop() {
        this.kernel.shutdown();
    }

    public void suspend() {

    }

    public void resume() {

    }

    /**
     * 引擎是否已启动。
     *
     * @return
     */
    public boolean hasStarted() {
        return this.started;
    }

    /**
     * 内核是否已就绪。
     *
     * @return
     */
    public boolean isReady() {
        return this.started && this.kernel.isReady();
    }

    /**
     * 获取联系人服务模块。
     *
     * @return
     */
    public ContactService getContactService() {
        return (ContactService) this.kernel.getModule(ContactService.NAME);
    }

    /**
     * 签入指定的联系人。
     *
     * @param contactId 指定联系人 ID 。
     * @param name 指定联系人名称。
     * @param context 指定联系人的上下文数据。
     * @param handler 指定回调句柄。
     * @return 如果返回 {@code false} 表示当前状态下不能进行该操作，请检查是否正确启动魔方。
     */
    public boolean signIn(Long contactId, String name, JSONObject context, SignHandler handler) {
        ContactService contactService = this.getContactService();
        Self self = new Self(contactId, name, context);
        return contactService.signIn(self, handler);
    }
}
