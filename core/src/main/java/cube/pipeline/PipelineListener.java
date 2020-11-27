package cube.pipeline;

import org.json.JSONObject;

import cube.service.model.State;

/**
 * 数据通道监听器。
 *
 * @author LiuFeng
 * @data 2020/8/26 14:28
 */
public interface PipelineListener {

    /**
     * 收到数据
     *
     * @param service 服务节点
     * @param action  动作
     * @param state   状态
     * @param packet  数据
     */
    void onReceived(String service, String action, State state, Packet packet);

    /**
     * 收到数据
     *
     * @param service 服务节点
     * @param action  动作
     * @param state   状态
     * @param data    数据
     */
//    void onReceived(String service, String action, State state, JSONObject data);
}
