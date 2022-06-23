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

import static android.content.Context.BIND_AUTO_CREATE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import cube.auth.AuthService;
import cube.auth.event.ResetAuthConfigEvent;
import cube.auth.model.AuthDomain;
import cube.contact.ContactService;
import cube.contact.ContactServiceEvent;
import cube.contact.handler.SignHandler;
import cube.contact.model.Self;
import cube.core.Kernel;
import cube.core.KernelConfig;
import cube.core.ModuleError;
import cube.core.handler.KernelHandler;
import cube.engine.handler.EngineHandler;
import cube.engine.service.CubeService;
import cube.engine.util.Promise;
import cube.ferry.FerryService;
import cube.fileprocessor.FileProcessor;
import cube.filestorage.FileStorage;
import cube.messaging.MessagingService;
import cube.multipointcomm.MultipointComm;
import cube.util.FileUtils;
import cube.util.LogUtils;
import cube.util.ObservableEvent;
import cube.util.Observer;

/**
 * 魔方引擎 API 入口类。
 */
public class CubeEngine implements Observer {

    protected static CubeEngine instance = null;

    private KernelConfig config;

    private Kernel kernel;

    private AtomicBoolean starting;

    private AtomicBoolean started;

    private CubeEngine() {
        this.starting = new AtomicBoolean(false);
        this.started = new AtomicBoolean(false);
        this.kernel = new Kernel();
        this.kernel.installModule(new AuthService());
        this.kernel.installModule(new ContactService());
        this.kernel.installModule(new FileStorage());
        this.kernel.installModule(new FileProcessor());
        this.kernel.installModule(new MessagingService());
        this.kernel.installModule(new MultipointComm());
        this.kernel.installModule(new FerryService());
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
     * @return 返回是否进入了启动流程。
     */
    public boolean start(Context context, EngineHandler handler) {
        if (this.starting.get()) {
            return false;
        }

        this.starting.set(true);

        LogUtils.i("CubeEngine", "#start : " + this.config.print());

        // 线程池赋值
        Promise.setExecutor(Executors.newCachedThreadPool());

        boolean processed = this.kernel.startup(context, this.config, new KernelHandler() {
            @Override
            public void handleCompletion(Kernel kernel) {
                LogUtils.i("CubeEngine", "Cube engine started");

                started.set(true);

                // 启动联系人模块
                getContactService().start();

                handler.handleSuccess(CubeEngine.instance);

                starting.set(false);
            }

            @Override
            public void handleFailure(ModuleError error) {
                // 启动失败
                started.set(false);
                starting.set(false);

                LogUtils.i("CubeEngine", "Cube engine start failed : " + error.code);
                handler.handleFailure(error.code, (null != error.description) ? error.description : error.moduleName);
            }
        });

        if (processed) {
            this.getAuthService().attachWithName(ResetAuthConfigEvent.NAME, this);
            this.getContactService().attachWithName(ContactServiceEvent.SignIn, this);
        }
        else {
            this.starting.set(false);
        }

        return processed;
    }

    public void stop() {
        LogUtils.i("CubeEngine", "#stop : " + this.config.domain);

        this.kernel.shutdown();

        Promise.getExecutor().shutdown();

        this.getContactService().detachWithName(ContactServiceEvent.SignIn, this);
        this.getAuthService().detachWithName(ResetAuthConfigEvent.NAME, this);

        this.starting.set(false);
        this.started.set(false);
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
        return this.started.get();
    }

    /**
     * 内核是否已就绪。
     *
     * @return
     */
    public boolean isReady() {
        return this.started.get() && this.kernel.isReady();
    }

    /**
     * 获取授权管理模块。
     *
     * @return 返回授权管理模块。
     */
    public AuthService getAuthService() {
        return (AuthService) this.kernel.getModule(AuthService.NAME);
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
     * 获取多方实时音视频通讯模块。
     *
     * @return 返回多方实时音视频通讯模块。
     */
    public MultipointComm getMultipointComm() {
        return (MultipointComm) this.kernel.getModule(MultipointComm.NAME);
    }

    /**
     * 获取摆渡服务模块。
     *
     * @return 返回摆渡服务模块。
     */
    public FerryService getFerryService() {
        return (FerryService) this.kernel.getModule(FerryService.NAME);
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

    /**
     * 重置配置。
     *
     * @param activity 指定当前操作的 Activity 实例。
     * @param authDomain 指定新的访问域。
     * @param serviceConnection 服务连接。
     * @return 返回是否写入配置文件成功。
     */
    public boolean resetConfig(Activity activity, AuthDomain authDomain,
                            ServiceConnection serviceConnection) {
        // 写入配置文件
        File path = FileUtils.getFilePath(activity, "cube");
        File configFile = new File(path, "cube.config");
        JSONObject data = new JSONObject();
        try {
            data.put("CUBE_ADDRESS", authDomain.mainEndpoint.host);
            data.put("CUBE_PORT", authDomain.mainEndpoint.port);
            data.put("CUBE_DOMAIN", authDomain.domainName);
            data.put("CUBE_APPKEY", authDomain.appKey);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (!FileUtils.writeJSONFile(configFile, data)) {
            LogUtils.w("CubeEngine", "#resetConfig - Write config file failed: "
                    + configFile.getName());
            return false;
        }

        activity.runOnUiThread(() -> {
            Intent intent = new Intent(activity, CubeService.class);
            intent.setAction(CubeService.ACTION_RESET);
            activity.startService(intent);

            Intent bindIntent = new Intent(activity, CubeService.class);
            activity.bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE);
        });

        return true;
    }

    /**
     * 读取内核配置信息。
     *
     * @param context 应用程序上下文。
     * @return 返回内核配置。
     */
    public KernelConfig loadConfig(Context context) {
        KernelConfig config = null;
        try {
            File path = FileUtils.getFilePath(context, "cube");
            File configFile = new File(path, "cube.config");
            if (configFile.exists()) {
                LogUtils.d("CubeEngine", "#loadConfig - Load config from config file");

                JSONObject data = FileUtils.readJSONFile(configFile);
                String address = data.getString("CUBE_ADDRESS");
                int port = data.getInt("CUBE_PORT");
                String domain = data.getString("CUBE_DOMAIN");
                String appKey = data.getString("CUBE_APPKEY");
                config = new KernelConfig(address, port, domain, appKey);
            }
            else {
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(),
                        PackageManager.GET_META_DATA);
                String address = appInfo.metaData.getString("CUBE_ADDRESS");
                int port = appInfo.metaData.containsKey("CUBE_PORT") ? appInfo.metaData.getInt("CUBE_PORT") : 7000;
                String domain = appInfo.metaData.getString("CUBE_DOMAIN");
                String appKey = appInfo.metaData.getString("CUBE_APPKEY");

                if (null != address && null != domain && null != appKey) {
                    config = new KernelConfig(address, port, domain, appKey);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return config;
    }

    /**
     * 保存新的配置数据。
     *
     * @param context
     * @param address
     * @param port
     * @param domainName
     * @param appKey
     * @return
     */
    public boolean saveConfig(Context context, String address, int port,
                           String domainName, String appKey) {
        // 写入配置文件
        File path = FileUtils.getFilePath(context, "cube");
        File configFile = new File(path, "cube.config");
        JSONObject data = new JSONObject();
        try {
            data.put("CUBE_ADDRESS", address);
            data.put("CUBE_PORT", port);
            data.put("CUBE_DOMAIN", domainName);
            data.put("CUBE_APPKEY", appKey);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (!FileUtils.writeJSONFile(configFile, data)) {
            LogUtils.w("CubeEngine", "#saveConfig - Write config file failed: "
                    + configFile.getName());
            return false;
        }
        else {
            LogUtils.i("CubeEngine", "#saveConfig success");
            return true;
        }
    }

    @Override
    public void update(ObservableEvent event) {
        if (ContactServiceEvent.SignIn.equals(event.getName())) {
            // 账号签入成功，可进行一些数据更新操作，默认不需要，各模块会自动完成更新
        }
        else if (ResetAuthConfigEvent.NAME.equals(event.getName())) {
            // 加载默认配置
            this.config = loadConfig(this.kernel.getContext());

            Activity activity = (Activity) event.getData();
            activity.runOnUiThread(() -> {
                Intent intent = new Intent(activity, CubeService.class);
                intent.setAction(CubeService.ACTION_RESET);
                activity.startService(intent);
            });
        }
    }
}
