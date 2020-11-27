package cube.whiteboard.service;

import java.io.File;
import java.util.List;

import cube.common.callback.Callback;
import cube.common.callback.CubeCallback0;
import cube.common.callback.CubeCallback1;
import cube.whiteboard.impl.WhiteboardFile;

/**
 * 白板服务
 *
 * @author LiuFeng
 * @data 2020/8/14 13:54
 */
public interface WhiteboardService {

    /**
     * 添加监听
     *
     * @param listener
     */
    void addListener(WhiteboardListener listener);

    /**
     * 移除监听
     *
     * @param listener
     */
    void removeListener(WhiteboardListener listener);

    /**
     * 初始化白板服务
     *
     * @param token    由SDK2.8白板模块获得
     * @param callback
     */
    void init(String token, CubeCallback0 callback);

    /**
     * 同步白板历史数据
     *
     * @param startTime 为零同步所有
     * @param callback
     */
    void syncData(long startTime, CubeCallback0 callback);

    /**
     * 加入
     *
     * @param wid      白板房间
     * @param callback
     */
    void join(String wid, CubeCallback1<WhiteboardView> callback);

    /**
     * 离开
     *
     * @param wid      白板房间
     * @param callback
     */
    void leave(String wid, CubeCallback1<Whiteboard> callback);

    /**
     * 获取白板画板
     *
     * @return
     */
    WhiteboardView getView();

    /**
     * 自动同步历史数据
     *
     * @param auto
     */
    void setAutoSyncData(boolean auto);

    /**
     * 是否自动同步历史数据
     *
     * @return
     */
    boolean isAutoSyncData();

    /**
     * 上传文件
     *
     * @param wid
     * @param file
     * @param callback
     */
    void upload(String wid, File file, CubeCallback1<WhiteboardFile> callback);

    /**
     * 获取文件列表
     *
     * @param wid
     * @param callback
     */
    void listFiles(String wid, CubeCallback1<List<WhiteboardFile>> callback);
}
