package cube.service;


import cube.service.model.CubeError;

/**
 * 引擎状态监听器
 *
 * @author workerinchina@163.com
 */
public interface CubeEngineListener {

    /**
     * 引擎启动完成
     */
    public void onStarted();

    /**
     * 引擎状态变化
     *
     * @param state
     */
    public void onStateChange(CubeState state);

    /**
     * 引擎停止
     */
    public void onStopped();

    /**
     * 引擎错误
     *
     * @param error
     */
    public void onFailed(CubeError error);
}
