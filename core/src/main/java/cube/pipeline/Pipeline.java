package cube.pipeline;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cube.common.callback.CubeCallback0;
import cube.core.EngineAgent;

/**
 * 数据通道服务接口
 *
 * @author LiuFeng
 * @data 2020/8/26 14:00
 */
public abstract class Pipeline {
    protected String name;
    protected String address;
    protected int port;
    protected String tokenCode;

    protected Map<String, List<PipelineListener>> listenerMap;

    protected Pipeline(String name) {
        this.name = name;
        this.listenerMap = new ConcurrentHashMap<>();
    }

    /**
     * 获取指定通道
     *
     * @param name
     * @return
     */
    public static Pipeline getPipeline(String name) {
        return EngineAgent.getInstance().getPipeline(name);
    }

    /**
     * 添加监听器。
     *
     * @param destination 指定监听的目标或识别串。
     * @param listener    指定通道监听器。
     */
    public void addListener(String destination, PipelineListener listener) {
        if (!TextUtils.isEmpty(destination) && listener != null) {
            List<PipelineListener> listeners = this.listenerMap.get(destination);
            if (listeners == null) {
                listeners = new ArrayList<>();
            }

            if (!listeners.contains(listener)) {
                listeners.add(listener);
                this.listenerMap.put(destination, listeners);
            }
        }
    }

    /**
     * 移除监听器。
     *
     * @param destination 指定监听的目标或识别串。
     * @param listener    指定通道监听器。
     */
    public void removeListener(String destination, PipelineListener listener) {
        List<PipelineListener> listeners = this.listenerMap.get(destination);
        if (listeners != null) {
            // 先从集合删除，当监听集合为空时再删除此服务监听
            if (listeners.remove(listener) && listeners.isEmpty()) {
                this.listenerMap.remove(destination);
            }
        }
    }

    /**
     * 获取通道名称
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * 设置服务的地址和端口。
     *
     * @param address 服务器访问地址。
     * @param port    服务器访问端口。
     */
    public void setRemoteAddress(String address, int port) {
        this.address = address;
        this.port = port;
    }

    /**
     * 设置令牌代码。
     *
     * @param tokenCode
     */
    public void setTokenCode(String tokenCode) {
        this.tokenCode = tokenCode;
    }

    /**
     * 初始化连接
     *
     * @param context
     */
    public abstract void init(Context context);

    /**
     * 启动数据通道。
     */
    public abstract void open();

    /**
     * 启动数据通道。
     */
    public abstract void open(CubeCallback0 callback);

    /**
     * 关闭数据通道。
     */
    public abstract void close();

    /**
     * 数据通道是否就绪
     *
     * @return
     */
    public abstract boolean isReady();

    /**
     * 发送数据
     *
     * @param service
     * @param action
     * @param param
     */
    public abstract void send(String service, String action, JSONObject param);

    /**
     * 发送数据。
     *
     * @param service     指定通道的发送目标或接收端识别串。
     * @param packet      指定待发送的数据包。
     */
    public abstract void send(String service, Packet packet);

    /**
     * 进入后台
     */
    public abstract void sleep();

    /**
     * 回到前台
     */
    public abstract void wakeup();
}
