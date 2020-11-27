package cube.message.service;

import java.util.List;

import cube.common.callback.CubeCallback1;
import cube.common.callback.CubeCallback2;
import cube.common.callback.CubeCallback3;
import cube.message.service.model.Message;
import cube.service.model.DeviceInfo;
import cube.message.service.model.PageInfo;

/**
 * 消息服务接口
 *
 * @author LiuFeng
 * @data 2020/9/2 16:21
 */
public interface MessageService {

    /**
     * 添加消息监听器
     *
     * @param listener
     */
    void addListener(MessageListener listener);

    /**
     * 删除消息监听器
     *
     * @param listener
     */
    void removeListener(MessageListener listener);

    /**
     * 发送消息
     *
     * @param message
     * @param callback
     */
    void sendMessage(Message message, CubeCallback1<Message> callback);

    /**
     * 回执消息
     *
     * @param sessionId
     * @param timestamp
     * @param callback
     */
    void receiptMessage(String sessionId, long timestamp, CubeCallback3<String, Long, DeviceInfo> callback);

    /**
     * 撤回消息
     *
     * @param sn
     * @param callback
     */
    void recallMessage(long sn, CubeCallback1<Message> callback);

    /**
     * 取消文件消息的上传或下载
     *
     * @param sn
     * @param callback
     */
    void cancelFileMessage(long sn, CubeCallback1<Message> callback);

    /**
     * 删除消息
     *
     * @param sn
     * @param callback
     */
    void deleteMessage(long sn, CubeCallback1<Message> callback);

    /**
     * 查询消息
     *
     * @param sns
     * @param callback
     *
     * @return
     */
    boolean queryMessage(List<Long> sns, CubeCallback1<List<Message>> callback);

    /**
     * 查询消息
     *
     * @param sessionId      聊天id
     * @param sinceTimestamp 开始时间
     * @param untilTimestamp 结束时间
     * @param offset         开始页码
     * @param count          每页条数
     * @param callback
     */
    void queryMessage(String sessionId, long sinceTimestamp, long untilTimestamp, int offset, int count, CubeCallback2<PageInfo, List<Message>> callback);

    /**
     * 同步消息
     */
    void syncMessage();
}

