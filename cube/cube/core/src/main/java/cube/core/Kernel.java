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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cell.util.Cryptology;
import cube.auth.AuthService;
import cube.auth.AuthToken;
import cube.auth.handler.AuthTokenHandler;
import cube.core.handler.KernelHandler;
import cube.pipeline.CellPipeline;

/**
 * 内核。内核管理所有的模块和通信管道。
 */
public class Kernel implements PipelineListener {

    private final static int MAX_THREADS = 4;

    private static Kernel defaultInstance;

    private Context context;

    private KernelConfig config;

    private boolean working;

    private String deviceSerial;

    private EntityInspector inspector;

    private Pipeline pipeline;

    private Map<String, Module> moduleMap;

    private ExecutorService executor;

    public Kernel() {
        this.working = false;
        this.moduleMap = new HashMap<>();
        this.executor = Executors.newFixedThreadPool(MAX_THREADS);
        this.inspector = new EntityInspector();
        this.defaultInstance = this;
    }

    public final static Kernel getDefault() {
        return Kernel.defaultInstance;
    }

    public boolean startup(Context context, KernelConfig config, KernelHandler handler) {
        if (null == config || null == handler || this.working) {
            return false;
        }

        this.working = true;

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
        this.pipeline.open();

        // 检测授权
        boolean ret = this.checkAuth(config, new AuthTokenHandler() {
            @Override
            public void handleSuccess(AuthToken authToken) {
                // 设置数据通道令牌
                pipeline.setTokenCode(authToken.code);

                handler.handleCompletion(Kernel.this);
            }

            @Override
            public void handleFailure(ModuleError error) {
                working = false;

                handler.handleFailure(error);
            }
        });

        return ret;
    }

    public void shutdown() {
        for (Module module : this.moduleMap.values()) {
            module.stop();
        }

        if (null != this.pipeline) {
            this.pipeline.close();
            this.pipeline = null;
        }

        this.executor.shutdown();
        this.executor = null;

        this.working = false;
    }

    public void suspend() {

    }

    public void resume() {

    }

    public boolean isReady() {
        AuthService service = (AuthService) this.getModule(AuthService.NAME);
        AuthToken authToken = service.getToken();
        
        return this.working && (null != authToken && authToken.isValid());
    }

    public AuthToken activeToken(Long contactId) {
        AuthService service = (AuthService) this.getModule(AuthService.NAME);
        AuthToken token = service.allocToken(contactId);
        if (null == token) {
            return null;
        }

        // 更新管道令牌码
        this.pipeline.setTokenCode(token.code);

        return token;
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

    public Pipeline getPipeline() {
        return this.pipeline;
    }

    public String getDeviceSerial() {
        return this.deviceSerial;
    }

    protected AuthToken getAuthToken() {
        AuthService service = (AuthService) this.getModule(AuthService.NAME);
        return service.getToken();
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
        // 生成设备序号
        String serial = Build.SERIAL;
        if (null == serial || serial.length() == 0 || serial.equalsIgnoreCase("unknown")) {
            serial = Settings.System.getString(context.getContentResolver(), Settings.System.ANDROID_ID);

            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (null != wifiManager && wifiManager.isWifiEnabled()) {
                WifiInfo info = wifiManager.getConnectionInfo();
                if (null != info) {
                    String macAddress = info.getMacAddress();
                    serial = serial + "-" + macAddress.replaceAll(":", "");
                }
            }
        }
        else {
            serial = serial + "-" +  Settings.System.getString(context.getContentResolver(), Settings.System.ANDROID_ID);
        }

        // 进行散列
        this.deviceSerial = printHexBinary(Cryptology.getInstance().hashWithMD5(serial.getBytes(StandardCharsets.UTF_8)));

        for (Module module : this.moduleMap.values()) {
            module.pipeline = this.pipeline;
        }
    }

    private static String printHexBinary(byte[] data) {
        StringBuilder r = new StringBuilder(data.length * 2);
        for (byte b : data) {
            r.append(String.format("%02X", (int)(b & 0xFF)));
        }
        return r.toString().toLowerCase(Locale.ROOT);
    }

    private boolean checkAuth(KernelConfig config, AuthTokenHandler handler) {
        if (!this.hasModule(AuthService.NAME)) {
            Log.i("Kernel", "Can NOT find auth module : " + AuthService.NAME);
            return false;
        }

        AuthService authService = (AuthService) this.getModule(AuthService.NAME);

        // 查找本地的令牌
        AuthToken token = authService.loadLocalToken(config.domain, config.appKey);
        if (null != token) {
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    handler.handleSuccess(token);
                }
            });
            return true;
        }

        // 从服务器申请令牌
        authService.check(config.domain, config.appKey, handler);
        return true;
    }

    @Override
    public void onReceived(Pipeline pipeline, String source, Packet packet) {
        // Nothing
    }

    @Override
    public void onOpened(Pipeline pipeline) {

    }

    @Override
    public void onClosed(Pipeline pipeline) {

    }

    @Override
    public void onFaultOccurred(Pipeline pipeline, int code, String description) {

    }
}
