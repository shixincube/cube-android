package cube.whiteboard.impl;

import org.json.JSONObject;

import cube.whiteboard.service.WhiteboardView;

/**
 * 白板js监听接口
 *
 * @author LiuFeng
 * @data 2020/8/19 10:14
 */
public interface WBJsListener {

    /**
     * 白板准备就绪
     *
     * @param view
     */
    void onReady(WhiteboardView view);

    /**
     * 白板数据变更
     *
     * @param wid
     * @param cmd
     */
    void onDataChange(String wid, JSONObject cmd);
}
