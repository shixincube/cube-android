package cube.core;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import cube.pipeline.Pipeline;
import cube.service.CubeEngineListener;
import cube.service.CubeState;
import cube.service.model.CubeConfig;
import cube.service.model.CubeSession;
import cube.service.model.DeviceInfo;

/**
 * 引擎代理类（用于内部模块调用）
 *
 * @author LiuFeng
 * @data 2019/5/8 11:48
 */
public abstract class EngineAgent extends Subject {

    /**
     * 获取EngineAgent实例
     *
     * @return
     */
    public static EngineAgent getInstance() {
        return EngineKernel.getInstance();
    }

    /**
     * 添加监听器
     *
     * @param listener
     */
    public abstract void addListener(CubeEngineListener listener);

    /**
     * 删除监听器
     *
     * @param listener
     */
    public abstract void removeListener(CubeEngineListener listener);

    /**
     * 此方法用于启动引擎，将引擎置为工作状态。
     * 调用该方法后引擎将完成各种初始化工作。
     *
     * @param context
     * @return
     */
    public abstract boolean startup(@NonNull Context context);

    /**
     * 此方法用于停止引擎，将引擎置为关闭状态。
     * 调用该方法后引擎将关闭各项功能，并释放内存。
     */
    public abstract void shutdown();

    /**
     * 引擎是否启动
     *
     * @return
     */
    public abstract boolean isStarted();

    /**
     * 对引擎进行指定的参数配置。
     *
     * @param config 指定配置参数对象。
     */
    public abstract void setCubeConfig(@NonNull CubeConfig config);

    /**
     * 返回当前的配置对象实例。
     *
     * @return 返回配置对象实例。
     */
    public abstract CubeConfig getCubeConfig();

    /**
     * 返回当前有效的会话对象实例。
     *
     * @return 返回会话对象实例。
     */
    public abstract CubeSession getSession();

    /**
     * 返回引擎当前状态。
     *
     * @return 返回引擎状态。
     */
    public abstract CubeState getCubeEngineState();

    /**
     * 获取当前设备信息
     *
     * @return
     */
    public abstract DeviceInfo getDeviceInfo();

    /**
     * 获取上下文信息
     *
     * @return
     */
    public abstract Context getContext();

    /**
     * 获取地理信息
     *
     * @return
     */
    public abstract Location getLocation();

    /**
     * 安装数据通道。
     *
     * @param pipeline 指定待安装的数据通道。
     */
    public abstract void installPipeline(Pipeline pipeline);

    /**
     * 卸载数据通道。
     *
     * @param pipeline 指定待卸载的数据通道。
     */
    public abstract void uninstallPipeline(Pipeline pipeline);

    /**
     * 获取指定名称的数据通道。
     *
     * @param name 指定数据通道名称。
     */
    public abstract Pipeline getPipeline(String name);

    /**
     * 加载服务
     *
     * @param service
     * @param serviceImpl
     * @param <T>
     */
    public abstract <T> void loadService(Class<T> service, Class<? extends T> serviceImpl);

    /**
     * 获取服务
     *
     * @param cls
     * @param <T>
     * @return
     */
    public abstract <T> T getService(Class<T> cls);

    /**
     * 进入后台
     */
    public abstract void sleep();

    /**
     * 回到前台
     */
    public abstract void wakeup();
}
