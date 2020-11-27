package cube.message.impl;

/**
 * 消息模块的指令动作
 *
 * @author LiuFeng
 * @data 2020/9/2 15:22
 */
public interface MessageAction {

    /**
     * 发送消息。
     */
    String Push = "push";

    /**
     * 拉取消息。
     */
    String Pull = "pull";

    /**
     * 收到在线消息通知
     */
    String Notify = "notify";

    /**
     * 回执消息
     */
    String Receipt = "receipt";

    /**
     * 撤回消息
     */
    String Recall = "recall";
}

