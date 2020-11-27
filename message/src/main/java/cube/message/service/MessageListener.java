package cube.message.service;

import java.util.List;
import java.util.Map;

import cube.message.service.model.Message;
import cube.service.model.DeviceInfo;

/**
 * 消息监听
 *
 * @author LiuFeng
 * @data 2020/9/7 18:42
 */
public interface MessageListener {

    /**
     * 消息已发送
     *
     * @param message
     */
    void onMessageSent(Message message);

    /**
     * 收到消息
     *
     * @param message
     */
    void onMessageReceived(Message message);

    /**
     * 消息已撤回
     *
     * @param message
     */
    void onMessageRecalled(Message message);

    /**
     * 消息已回执
     *
     * @param sessionId
     * @param timestamp
     * @param deviceInfo
     */
    void onMessageReceipted(String sessionId, long timestamp, DeviceInfo deviceInfo);

    /**
     * 消息同步
     *
     * @param messageMap 未拉取消息集合（key 聊天对象 value 未拉取消息数组）
     */
    void onMessagesSync(Map<String, List<Message>> messageMap);

    /**
     * 当消息处理失败时回调
     *
     * @param code
     * @param desc
     */
    void onMessageError(int code, String desc);
}
