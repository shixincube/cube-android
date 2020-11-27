package cube.message.service;

/**
 * 消息状态描述
 *
 * @author LiuFeng
 * @data 2020/9/2 15:29
 */
public enum MessageState {

    /**
     * 未知
     */
    None(0),

    /**
     * 发送中。
     */
    Sending(1),

    /**
     * 接收中
     */
    Receiving(2),

    /**
     * 成功
     */
    Succeed(3),

    /**
     * 失败
     */
    Fault(4);

    public int state;

    MessageState(int state) {
        this.state = state;
    }

    /**
     * 转化枚举
     *
     * @param state
     * @return
     */
    public static MessageState parse(int state) {
        for (MessageState messageState : values()) {
            if (messageState.state == state) {
                return messageState;
            }
        }

        return None;
    }
}
