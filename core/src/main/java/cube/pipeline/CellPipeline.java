package cube.pipeline;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import cell.api.Nucleus;
import cell.api.Speakable;
import cell.api.TalkListener;
import cell.api.TalkService;
import cell.core.talk.Primitive;
import cell.core.talk.TalkError;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.common.callback.CubeCallback0;
import cube.core.EngineAgent;
import cube.core.EngineKernel;
import cube.core.Service;
import cube.utils.broadcast.ConnectionChangeListener;
import cube.utils.broadcast.ConnectionChangeReceiver;
import cube.service.CubeState;
import cube.service.model.State;
import cube.utils.EmptyUtil;
import cube.utils.GsonUtil;
import cube.utils.NetworkUtil;
import cube.utils.SpUtil;
import cube.utils.log.LogUtil;

/**
 * 使用 Cell 进行通信的管道服务。
 *
 * @author LiuFeng
 * @data 2020/8/26 14:45
 */
public class CellPipeline extends Pipeline implements TalkListener, ConnectionChangeListener {
    private static final String TAG = "CellPipeline";

    private Nucleus nucleus;
    private TalkService talkService;
    private List<String> services;
    private boolean isReady;
    private CubeCallback0 openCallback;
    private ConnectServerCallTask serverCallTask;

    private long talkTime;  // 连接时间

    private final static long SPACE_TIME = 2 * 1000L;

    /**
     * 全部service
     **/
    private String[] allServices = new String[]{
            Service.AuthService,            // 授权
            Service.ContactService,         // 联系人
            Service.MessageService,         // 消息
            Service.WhiteboardService,      // 白板
    };

    public CellPipeline() {
        super("Cell");
        services = new ArrayList<>();
    }

    @Override
    public void init(Context context) {
        if (talkService == null) {
            synchronized (this) {
                if (talkService == null) {
                    // 初始化 Nucleus
                    nucleus = new Nucleus(context);
                    talkService = nucleus.getTalkService();
                    services.clear();
                    Collections.addAll(services, allServices);

                    for (String service : services) {
                        talkService.setListener(service, this);
                    }
                }
            }
            //添加网络变化监听器
            ConnectionChangeReceiver.getInstance().addConnectionChangeListener(this);
        }
    }

    @Override
    public void open() {
        connectServer(0, true);
    }

    @Override
    public void open(CubeCallback0 callback) {
        this.openCallback = callback;
        connectServer(0, true);
    }

    @Override
    public void close() {
        if (talkService != null) {
            talkService.hangup(address, port, true);
            talkService.stopAllServers();
            talkService = null;
        }
        if (nucleus != null) {
            nucleus.destroy();
            nucleus = null;
        }
        services.clear();
        //删除网络变化监听器
        ConnectionChangeReceiver.getInstance().removeConnectionChangeListener(this);
    }

    public boolean isInit() {
        return talkService != null;
    }

    @Override
    public boolean isReady() {
        return isReady;
    }

    @Override
    public void send(String service, String action, JSONObject param) {
        Packet packet = new Packet(action, param);
        send(service, packet);
    }

    @Override
    public void send(String service, Packet packet) {
        LogUtil.i("发送---模块:" + service + " 指令:" + packet.name + " 参数:" + LogUtil.getFormatJson(packet.data));

        // 就绪状态直接发送
        if (isReady) {
            talkService.speak(service, packet.toDialect(tokenCode));
            return;
        }

        // 未就绪时，判断引擎是否启动
        if (EngineAgent.getInstance().isStarted()) {
            LogUtil.i("恢复连接");
            open();
        } else {
            LogUtil.i("引擎未启动");
        }
    }

    @Override
    public void sleep() {

    }

    @Override
    public void wakeup() {

    }

    /**
     * Genie相关接收数据回调
     * 将接收数据路由给指定服务
     *
     * @param speakable
     * @param service
     * @param primitive
     */
    @Override
    public void onListened(Speakable speakable, String service, Primitive primitive) {
        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = getAction(dialect);
        JSONObject dataJson = getData(dialect);
        dataJson = dataJson.optJSONObject(Service.DATA);
        dataJson = dataJson != null ? dataJson : new JSONObject();
        JSONObject stateJson = /*dataJson.optJSONObject(Service.STATE)*/getState(dialect);
        State state = /*new State(Service.StateOk, "OK")*/GsonUtil.toBean(stateJson, State.class);
        Long sn = getSn(dialect);
        Packet packet = new Packet(sn, action, dataJson);

        LogUtil.i("接收数据 --> dialect:\n" + LogUtil.getFormatJson(dialect.toString()));

        // 打印长度不超过1000个字符
        String jsonData = dataJson.toString();
        String logData = jsonData.length() > 2000 ? jsonData.substring(0, 2000) : jsonData;
        LogUtil.i("接收数据---模块:" + service + " ---指令:" + action + " ---状态:" + stateJson + " ---数据:\n" + logData);

        if (listenerMap.containsKey(service)) {
            List<PipelineListener> listeners = listenerMap.get(service);
            if (EmptyUtil.isNotEmpty(listeners)) {
                for (PipelineListener listener : listeners) {
                    listener.onReceived(service, action, state, packet);
                }
            }
        }
    }

    /**
     * @param speakable
     * @param service
     * @param primitive
     */
    @Override
    public void onSpoke(Speakable speakable, String service, Primitive primitive) {
    }

    /**
     * 回执处理(将发送数据路由给各服务)
     *
     * @param speakable
     * @param service
     * @param primitive
     */
    @Override
    public void onAck(Speakable speakable, String service, Primitive primitive) {
    }

    /**
     * 超时处理(将超时数据路由给各服务)
     *
     * @param speakable
     * @param service
     * @param primitive
     */
    @Override
    public void onSpeakTimeout(Speakable speakable, String service, Primitive primitive) {
    }

    /**
     * 服务连接成功
     *
     * @param cellet
     * @param speakable
     */
    @Override
    public void onContacted(String cellet, Speakable speakable) {
        if (TextUtils.equals(Service.AuthService, cellet)) {
            LogUtil.i("服务连接成功:" + cellet);
            this.isReady = true;
            SpUtil.setTag(nucleus.getTag().asString());

            if (openCallback != null) {
                openCallback.onSuccess();
            }
        }
    }

    /**
     * 停止服务
     *
     * @param speakable
     */
    @Override
    public void onQuitted(Speakable speakable) {
        LogUtil.i("onQuitted --> 停止服务");
        this.isReady = false;
    }

    /**
     * 失败的操作
     *
     * @param speakable
     * @param talkError
     */
    @Override
    public void onFailed(Speakable speakable, TalkError talkError) {
        LogUtil.e("连接服务错误:" + talkError.getErrorCode());
        this.isReady = false;
        if (openCallback != null) {
            openCallback.onError(talkError.getErrorCode(), null);
        }
    }

    /**
     * 取消全部连接
     */
    public void disConnected() {
        checkException();
        hangUpCall();
    }

    public void hangUpCall() {
        if (isInit()) {
            talkService.hangup(address, port, true);
        }
    }

    /**
     * 网络变化监听
     *
     * @param isNetworkAvailable
     */
    @Override
    public void onConnectionChange(boolean isNetworkAvailable) {
        LogUtil.i("onConnectionChange:" + isNetworkAvailable);
        if (isInit() && SpUtil.isInitUser()) {
            if (isNetworkAvailable) {
                LogUtil.i(TAG, "onConnectionChange --> 有网络，检查登录");
            } else {
                releaseConnectionSchedule();
                //改变引擎状态为启动
                EngineKernel.getInstance().setCubeEngineState(CubeState.PAUSE);
            }
        }
    }

    @Override
    public void onTimeTick(int interval5min) {
        //每隔五分钟会判断一次底层通信库是否连接
        if (isNetworkAvailable() && isInit() && !isReady()) {
            // todo
        }
    }

    /**
     * 连接服务
     *
     * @param spaceTime 连接延迟时间
     * @param force     是否强制连接
     */
    public synchronized void connectServer(long spaceTime, boolean force) {
        // 无网络时不连接
        if (!isNetworkAvailable()) {
            LogUtil.e(TAG, "can not connectServer, network is not available");
            return;
        }

        LogUtil.i(TAG, "connectServer--> spaceTime:" + spaceTime + " force:" + force + " serverCallTask:" + serverCallTask);

        // 强制连接时先释放连接
        if (force) {
            releaseConnectionSchedule();
        }

        LogUtil.i(TAG, "connectServer--> serverCallTask:" + serverCallTask);
        if (serverCallTask == null) {
            serverCallTask = new ConnectServerCallTask();
            LogUtil.i(TAG, "cc createConnectionSchedule 1");
            ConnectionExecutor.schedule(serverCallTask, spaceTime);
        } else {
            // 超时6秒连接后释放重连
            if (System.currentTimeMillis() - talkTime > SPACE_TIME) {
                LogUtil.i(TAG, "fixed some time serverCallTask not release Bug");
                releaseConnectionSchedule();
                // 超时释放连接后重新连接
                connectServer(spaceTime, force);
            }
        }
    }

    /**
     * 取消连接任务
     */
    private synchronized void releaseConnectionSchedule() {
        if (serverCallTask != null) {
            LogUtil.i("releaseConnectionSchedule");
            ConnectionExecutor.cancelSchedule(serverCallTask);
            disConnected();
            serverCallTask = null;
        }
    }

    /**
     * 连接服务任务
     * 如果有网会立刻尝试重连，如果没有网络
     * 则每多尝试一次就比上一次多一倍的间隔时间
     */
    private class ConnectServerCallTask extends TimerTask {
        @Override
        public void run() {
            LogUtil.i("ConnectServerCallTask--> host:" + address + " port:" + port);

            // 未初始化则结束
            if (TextUtils.isEmpty(address) || port <= 0) {
                LogUtil.i("please init license! host:" + address + " port:" + port);
                return;
            }

            // 无网络则结束
            if (!isNetworkAvailable()) {
                LogUtil.i("cannot connect server, network is not available");
                releaseConnectionSchedule();
                //引擎暂停连接服务器工作，切换状态为暂停
                EngineKernel.getInstance().setCubeEngineState(CubeState.PAUSE);
                return;
            }

            // 连接服务
            talkTime = System.currentTimeMillis();
            LogUtil.i(TAG, "start to call --> services:" + services);
            talkService.call(address, port, services);
        }
    }

    /**
     * 网络是否可用
     *
     * @return
     */
    private boolean isNetworkAvailable() {
        return EngineAgent.getInstance().getContext() != null && NetworkUtil.isNetAvailable(EngineAgent.getInstance().getContext());
    }

    /**
     * 数据解析--Param
     *
     * @param dialect
     * @return
     */
    private JSONObject getParam(ActionDialect dialect) {
        if (dialect.containsParam(Service.PARAM)) {
            return dialect.getParamAsJson(Service.PARAM);
        }
        return new JSONObject();
    }

    /**
     * 数据解析--State
     *
     * @param dialect
     * @return
     */
    private JSONObject getState(ActionDialect dialect) {
        if (dialect.containsParam(Service.STATE)) {
            return dialect.getParamAsJson(Service.STATE);
        }
        return new JSONObject();
    }

    /**
     * 数据解析--Data
     *
     * @param dialect
     * @return
     */
    private JSONObject getData(ActionDialect dialect) {
        if (dialect.containsParam(Service.DATA)) {
            return dialect.getParamAsJson(Service.DATA);
        }
        return new JSONObject();
    }

    /**
     * 数据解析--Action
     *
     * @param dialect
     * @return
     */
    private String getAction(ActionDialect dialect) {
        return dialect.getName();
    }

    /**
     * 数据解析--sn
     *
     * @param dialect
     * @return
     */
    private Long getSn(ActionDialect dialect) {
        if (dialect.containsParam(Service.SN)) {
            return dialect.getParamAsLong(Service.SN);
        }

        return null;
    }

    /**
     * 检查抛出未初始化的异常
     */
    private void checkException() {
        if (talkService == null) {
            throw new RuntimeException("pipeline is not init");
        }
    }

    /**
     * 连接服务的线程执行者
     */
    private static class ConnectionExecutor {

        private static Map<Runnable, ScheduledFuture> futures = new ConcurrentHashMap<>();

        /**
         * 单线程任务池
         **/
        private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable r) {
                return new Thread(r, "CubeEngine-ConnectionService");
            }
        });

        /**
         * 定时任务，适用于定时的操作
         *
         * @param runnable
         */
        public static void schedule(Runnable runnable, long delay) {
            ScheduledFuture<?> future = executor.schedule(runnable, delay, TimeUnit.MILLISECONDS);
            futures.put(runnable, future);
        }

        /**
         * 取消定时任务，适用于定时的操作
         *
         * @param runnable
         */
        public static void cancelSchedule(Runnable runnable) {
            if (futures.containsKey(runnable)) {
                ScheduledFuture<?> future = futures.remove(runnable);
                future.cancel(true);
            }
        }
    }
}
