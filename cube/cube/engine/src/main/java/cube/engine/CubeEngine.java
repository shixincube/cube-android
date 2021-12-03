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

package cube.engine;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.util.concurrent.Executors;

import cube.auth.AuthService;
import cube.contact.ContactService;
import cube.contact.ContactServiceEvent;
import cube.contact.handler.SignHandler;
import cube.contact.model.Self;
import cube.core.Kernel;
import cube.core.KernelConfig;
import cube.core.ModuleError;
import cube.core.handler.KernelHandler;
import cube.engine.handler.EngineHandler;
import cube.engine.util.Promise;
import cube.fileprocessor.FileProcessor;
import cube.filestorage.FileStorage;
import cube.messaging.MessagingService;
import cube.util.ObservableEvent;
import cube.util.Observer;

/**
 * 魔方引擎 API 入口类。
 */
public class CubeEngine implements Observer {

    protected static CubeEngine instance = null;

    private KernelConfig config;

    private Kernel kernel;

    private boolean started;

    private CubeEngine() {
        this.started = false;
        this.kernel = new Kernel();
        this.kernel.installModule(new AuthService());
        this.kernel.installModule(new ContactService());
        this.kernel.installModule(new FileStorage());
        this.kernel.installModule(new FileProcessor());
        this.kernel.installModule(new MessagingService());
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

        // 线程池赋值
        Promise.setExecutor(Executors.newCachedThreadPool());

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

        this.getContactService().attachWithName(ContactServiceEvent.SignIn, this);

        return this.started;
    }

    public void stop() {
        this.kernel.shutdown();

        Promise.getExecutor().shutdown();

        this.getContactService().detachWithName(ContactServiceEvent.SignIn, this);
    }

    public void suspend() {
        this.kernel.suspend();
    }

    public void resume() {
        this.kernel.resume();
    }

    public void warmup() {
        // 让最近的最前面的会话预加载数据
        // 预加载最近 10 个会话的消息，每个会话预加载 10 条
        this.getMessagingService().setPreloadConversationMessageNum(10, 10);
        this.getMessagingService().getRecentConversations();

        // 加载当前联系人的根目录
        this.getFileStorage().getSelfRoot();
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
     * @return 返回联系人服务模块。
     */
    public ContactService getContactService() {
        return (ContactService) this.kernel.getModule(ContactService.NAME);
    }

    /**
     * 获取文件存储服务模块。
     *
     * @return 返回文件存储服务模块。
     */
    public FileStorage getFileStorage() {
        return (FileStorage) this.kernel.getModule(FileStorage.NAME);
    }

    /**
     * 获取文件处理器服务模块。
     *
     * @return 返回文件处理器服务模块。
     */
    public FileProcessor getFileProcessor() {
        return (FileProcessor) this.kernel.getModule(FileProcessor.NAME);
    }

    /**
     * 获取消息传输服务模块。
     *
     * @return 返回消息传输服务模块。
     */
    public MessagingService getMessagingService() {
        return (MessagingService) this.kernel.getModule(MessagingService.NAME);
    }

    /**
     * 是否已经有账号签入。
     *
     * @return 如果有账号签入返回 {@code true} 。
     */
    public boolean hasSignIn() {
        ContactService contactService = this.getContactService();
        return (null != contactService.getSelf());
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

    @Override
    public void update(ObservableEvent event) {
        if (ContactServiceEvent.SignIn.equals(event.getName())) {
            // 账号签入成功，可进行一些数据更新操作，默认不需要，各模块会自动完成更新
        }
    }
}
