package cube.service;

/**
 * 引擎状态
 *
 * @author workerinchina@163.com
 */
public enum CubeState {

    /**
     * 启动: 引擎已启动，所有模块初始化完成后的状态
     */
    START,

    /**
     * 暂停: 没有网络时，启动后无法工作时的状态
     */
    PAUSE,

    /**
     * 就绪: 引擎登录成功，可以收发数据的状态---引擎断网或者引擎无法收发数据时会回到START状态
     */
    READY,

    /**
     * 工作中: 当有有超过一条消息需要接收处理时，处理数据过程中处于此状态---数据收发完成后回到READY状态
     */
    BUSY,

    /**
     * 停止: 默认值或引擎shutdown之后的状态
     */
    STOP
}
