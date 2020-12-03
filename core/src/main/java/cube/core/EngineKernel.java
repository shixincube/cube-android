package cube.core;

import android.content.Context;
import android.location.Location;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.NonNull;
import cube.auth.service.AuthService;
import cube.auth.service.AuthToken;
import cube.common.callback.CubeCallback0;
import cube.common.callback.CubeCallback1;
import cube.core.manager.LifecycleManager;
import cube.pipeline.CellPipeline;
import cube.pipeline.Pipeline;
import cube.service.CubeEngineListener;
import cube.service.CubeState;
import cube.service.model.CubeConfig;
import cube.service.model.CubeError;
import cube.service.model.CubeSession;
import cube.service.model.DeviceInfo;
import cube.utils.EmptyUtil;
import cube.utils.GsonUtil;
import cube.utils.SpUtil;
import cube.utils.ThreadUtil;
import cube.utils.UIHandler;
import cube.utils.Utils;
import cube.utils.broadcast.ConnectionChangeReceiver;
import cube.utils.log.LogUtil;

/**
 * 引擎内核
 *
 * @author LiuFeng
 * @data 2019/5/8 11:49
 */
public class EngineKernel extends EngineAgent {
    private static final String TAG = "EngineKernel";

    private static EngineKernel instance = new EngineKernel();

    private boolean isStarted;
    private boolean isStarting;
    private Context context;
    private DeviceInfo deviceInfo;
    private CubeSession session;
    private CubeConfig cubeConfig;
    private KernelConfig kernelConfig;
    private Pipeline defaultPipeline;

    // 监听
    private List<CubeEngineListener> listeners;

    // 服务关系
    private Map<Class<?>, Class<?>> relationMap;

    // 服务实例
    private Map<Class<?>, Module> servicesMap;

    // 通道实例
    private Map<String, Pipeline> pipelineMap;

    /**
     * 构造函数
     */
    private EngineKernel() {
        session = new CubeSession();
        cubeConfig = new CubeConfig();
        listeners = new ArrayList<>();
        defaultPipeline = new CellPipeline();
        relationMap = new ConcurrentHashMap<>();
        servicesMap = new ConcurrentHashMap<>();
        pipelineMap = new ConcurrentHashMap<>();
    }

    /**
     * 单例获取
     *
     * @return
     */
    public static EngineKernel getInstance() {
        return instance;
    }

    @Override
    public void addListener(CubeEngineListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(CubeEngineListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    @Override
    public synchronized boolean startup(@NonNull Context context, KernelConfig config) {
        LogUtil.i(TAG, "startup --> start");

        if (isStarting || isStarted) {
            LogUtil.i(TAG, "CubeEngine already startup");
            return false;
        }

        try {
            init(context.getApplicationContext());

            //引擎shutdown之后的再次startup 如果用户信息初始化过自动去连接
            if (cubeConfig.isAlwaysOnline() && SpUtil.isInitUser()) {
                wakeup();
            }

            UIHandler.run(() -> {
                for (int i = 0; i < listeners.size(); i++) {
                    listeners.get(i).onStarted();
                }
            });

            setCubeEngineState(CubeState.START);

            LifecycleManager.getInstance().registerActivityLifecycle(context);
            LogUtil.i(TAG, "startup --> end");

            return true;
        } catch (final Exception e) {
            isStarting = false;
            LogUtil.e(TAG, "startup --> Failed:", e);
            UIHandler.run(() -> {
                for (int i = 0; i < listeners.size(); i++) {
                    listeners.get(i).onFailed(new CubeError(0, e.getMessage()));
                }
            });
        }
        return false;
    }

    /**
     * 初始化
     *
     * @param context
     */
    private void init(Context context) {
        this.isStarting = true;
        this.context = context;

        SpUtil.init(context);

        // 初始化引擎配置信息
        initConfig();

        boolean isDebug = cubeConfig.isDebug() || SpUtil.isDebug();

        // 初始化日志工具
        if (isDebug) {
            String logPath = SpUtil.getLogResourcePath();
            LogUtil.addCommonLogHandle();
            LogUtil.addDiskLogHandle(context, logPath);
            LogUtil.setLogTag("CubeEngine");
            LogUtil.setLoggable(true);
        } else {
            LogUtil.removeAllHandles();
            LogUtil.setLoggable(false);
        }

        LogUtil.i("CubeEngine#Startup Version:" /*+ Version.getDescription()*/ + " CC:" + cell.core.Version.getNumbers());
        LogUtil.d(TAG, "Android SDK: " + Build.VERSION.SDK_INT + ", " + "Release: " + Build.VERSION.RELEASE + ", " + "Brand: " + Build.BRAND + ", " + "Device: " + Build.DEVICE + ", " + "Id: " + Build.ID + ", " + "Hardware: " + Build.HARDWARE + ", " + "Manufacturer: " + Build.MANUFACTURER + ", " + "Model: " + Build.MODEL + ", " + "Product: " + Build.PRODUCT);

        // 注册网络广播监听
        ConnectionChangeReceiver.getInstance().register(context);

        // 销毁旧模块实例
        destroyModules();

        // 创建模块实例
        createModels();

        // 初始化服务
        initModules();

        this.isStarted = true;
        this.isStarting = false;
    }

    @Override
    public synchronized void shutdown() {
        LogUtil.i("CubeEngine-->shutdown: start");
        ConnectionChangeReceiver.getInstance().unregister(context);
        LifecycleManager.getInstance().unregisterActivityLifecycle(context);

        destroyModules();

        this.isStarting = false;
        this.isStarted = false;
        ThreadUtil.releaseSchedules();

        UIHandler.run(() -> {
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).onStopped();
            }
        });
        UIHandler.dispose();
        LogUtil.i("CubeEngine-->shutdown: end");
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    @Override
    public void setCubeConfig(@NonNull CubeConfig config) {
        this.cubeConfig = config;

        //重置config
        initConfig();
    }

    private void initConfig() {
        if (null != cubeConfig && null != context) {
            SpUtil.setTransportProtocol(cubeConfig.getTransportProtocol());
            SpUtil.setSupportSip(cubeConfig.isSupportSip());
            SpUtil.setDebug(cubeConfig.isDebug());
            if (cubeConfig.getResourceDir() != null) {
                SpUtil.setResourcePath(cubeConfig.getResourceDir());
            }
            if (null != cubeConfig.getAudioCodec()) {
                SpUtil.setAudioCodec(cubeConfig.getAudioCodec());
            }
            if (null != cubeConfig.getVideoCodec()) {
                SpUtil.setVideoCodec(cubeConfig.getVideoCodec());
            }
            if (null != cubeConfig.getLicenseServer()) {
                SpUtil.setLicenseServer(cubeConfig.getLicenseServer());
            }
            if (cubeConfig.getCameraId() >= 0) {
                SpUtil.setVideoId(cubeConfig.getCameraId());
            }
        } else {
            LogUtil.i("config/mContext is null");
        }
    }

    @Override
    public CubeConfig getCubeConfig() {
        return cubeConfig;
    }

    @Override
    public CubeSession getSession() {
        return session;
    }

    @Override
    public CubeState getCubeEngineState() {
        return session.state;
    }

    /**
     * 获取当前设备
     *
     * @return
     */
    @Override
    public DeviceInfo getDeviceInfo() {
        if (deviceInfo == null && context != null) {
            deviceInfo = Utils.getDevice(context);
            deviceInfo.setCubeId(SpUtil.getCubeId());
        }
        return deviceInfo;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public Location getLocation() {
        return GsonUtil.toBean(SpUtil.getLocation(), Location.class);
    }

    public void setCubeEngineState(CubeState state) {
        if (session.state != state) {
            LogUtil.i("changeState --> EngineState:" + state);
            if (state == CubeState.READY) {
                return;
            }

            session.state = state;
            UIHandler.run(() -> {
                for (int i = 0; i < listeners.size(); i++) {
                    listeners.get(i).onStateChange(session.state);
                }
            });
        }
    }

    @Override
    public void installPipeline(Pipeline pipeline) {
        if (pipeline != null) {
            pipelineMap.put(pipeline.getName(), pipeline);
        }
    }

    @Override
    public void uninstallPipeline(Pipeline pipeline) {
        if (pipeline != null) {
            pipelineMap.remove(pipeline.getName());
        }
    }

    @Override
    public Pipeline getPipeline(String name) {
        return pipelineMap.get(name);
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public <T> void loadService(Class<T> service, Class<? extends T> serviceImpl) {
        LogUtil.i(TAG, "loadService --> service:" + service.getSimpleName());
        relationMap.put(service, serviceImpl);
        createService(service, serviceImpl);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> cls) {
        if (cls == null) {
            return null;
        }

        LogUtil.i(TAG, "getService --> " + cls.getSimpleName());

        T service = (T) servicesMap.get(cls);
        if (service != null) {
            return service;
        }

        // 引擎未启动时抛异常
        if (!isStarted) {
            throw new RuntimeException("CubeEngine is not startup");
        }

        // 初始化关系数据
        if (relationMap.isEmpty()) {
            createModels();
        }

        return (T) servicesMap.get(cls);
    }

    @Override
    public void sleep() {
        for (Pipeline pipeline : pipelineMap.values()) {
            pipeline.sleep();
        }
    }

    @Override
    public void wakeup() {
        for (Pipeline pipeline : pipelineMap.values()) {
            pipeline.wakeup();
        }
    }

    /**
     * 创建模块实例
     */
    private void createModels() {
        // 安装通信通道
        installPipeline(defaultPipeline);

        // 加载模块服务
        loadClass(Loader.AuthLoader);
        loadClass(Loader.ContactLoader);
        loadClass(Loader.MessageLoader);
        loadClass(Loader.WhiteboardLoader);
    }

    /**
     * 检查模块依赖
     */
    private void checkDepend() {
        // 取出全部模块名称
        List<String> modules = new ArrayList<>();
        for (Class<?> cls : servicesMap.keySet()) {
            modules.add(cls.getSimpleName());
        }

        // 校验依赖关系
        for (Module service : servicesMap.values()) {
            List<String> requires = service.getRequires();
            if (!EmptyUtil.isEmpty(requires)) {
                for (String modelName : requires) {
                    if (!modules.contains(modelName)) {
                        LogUtil.e(String.format("Module %s is not installed, which module %s depends.", modelName, service.getName()));
                    }
                }
            }
        }
    }

    /**
     * 初始化各模块
     */
    private void initModules() {
        initPipeline();

        // 检查模块依赖
        checkDepend();

        // 给各模块绑定默认通信管道
        for (Module cubeService : servicesMap.values()) {
            cubeService.bindPipeline(defaultPipeline);
        }

        // 启动各服务模块
        for (Module cubeService : servicesMap.values()) {
            cubeService.start();
        }

        // 检查授权
        checkAuth(kernelConfig);
    }

    private void checkAuth(KernelConfig config) {
        // 配置并启动默认管道
        defaultPipeline.setRemoteAddress(config.address, config.port);
        defaultPipeline.open(new CubeCallback0() {
            @Override
            public void onSuccess() {
                // 管道打开后，检查授权
                getService(AuthService.class).checkToken(config.domain, config.appKey, config.address, new CubeCallback1<AuthToken>() {
                    @Override
                    public void onSuccess(AuthToken token) {
                        LogUtil.i(TAG, "applyToken --> token:" + token);
                        defaultPipeline.setTokenCode(token.code);
                    }

                    @Override
                    public void onError(int code, String desc) {
                        LogUtil.i(TAG, "applyToken --> code:" + code + " desc:" + desc);
                    }
                });
            }

            @Override
            public void onError(int code, String desc) {
                LogUtil.i(TAG, "checkAuth --> open pipeline failed. code:" + code + " desc:" + desc);
            }
        });
    }

    /**
     * 销毁各模块
     */
    private void destroyModules() {
        destroyPipeline();

        // 停止并销毁各服务模块
        if (!servicesMap.isEmpty()) {
            for (Module cubeService : servicesMap.values()) {
                cubeService.stop();
            }
            servicesMap.clear();
        }
    }

    /**
     * 初始化通信管道
     */
    private synchronized void initPipeline() {
        for (Pipeline pipeline : pipelineMap.values()) {
            pipeline.init(context);
        }
    }

    /**
     * 销毁通信管道
     */
    private synchronized void destroyPipeline() {
        for (Pipeline pipeline : pipelineMap.values()) {
            pipeline.close();
        }
        pipelineMap.clear();
    }

    /**
     * 创建服务
     *
     * @param service
     * @param serviceImpl
     */
    private synchronized void createService(Class<?> service, Class<?> serviceImpl) {
        // 防止创建多个实例
        if (servicesMap.containsKey(service)) {
            return;
        }

        try {
            Module cubeService = (Module) Class.forName(serviceImpl.getName()).getConstructor(Context.class).newInstance(context);
            servicesMap.put(service, cubeService);
        } catch (Exception e) {
            LogUtil.i(TAG, "引擎初始化服务失败：" + service.getSimpleName());
        }
    }

    /**
     * 类加载
     * 描述：通过类加载，在对应类静态代码块中注册服务模块，
     * 目的是实现模块的隔离和动态加载
     *
     * @param className
     */
    private static void loadClass(String className) {
        try {
            Class.forName(className);
        } catch (Exception ignored) {
        }
    }
}
