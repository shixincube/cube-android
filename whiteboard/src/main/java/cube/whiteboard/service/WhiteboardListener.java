package cube.whiteboard.service;

/**
 * 白板监听接口
 *
 * @author LiuFeng
 * @data 2020/8/20 10:13
 */
public interface WhiteboardListener {

    /**
     * 其他人加入
     *
     * @param wid
     * @param uid
     */
    void onJoined(String wid, String uid);

    /**
     * 其他人离开
     *
     * @param wid
     * @param uid
     */
    void onLeaved(String wid, String uid);

    /**
     * 白板就绪
     */
    void onReady();

    /**
     * 白板历史数据同步完成
     */
    void onSyncDataCompleted();

    /**
     * 白板错误
     *
     * @param code
     * @param desc
     */
    void onError(int code, String desc);
}
