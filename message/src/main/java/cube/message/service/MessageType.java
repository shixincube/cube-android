package cube.message.service;

/**
 * 消息类型描述
 *
 * @author LiuFeng
 * @data 2020/9/8 14:33
 */
public enum MessageType {
    /**
     * 未知类型的消息
     */
    UnKnown("unknown"),

    /**
     * 文本类型的消息
     */
    Text("text"),

    /**
     * 卡片类型的消息
     */
    Card("card"),

    /**
     * 文件类型的消息
     */
    File("file"),

    /**
     * 图片类型的消息
     */
    Image("image"),

    /**
     * 自定义类型的消息
     */
    Custom("custom"),

    /**
     * 语音类型的消息
     */
    Voice("voice"),

    /**
     * 视频类型的消息
     */
    Video("video"),

    /**
     * 历史类型的消息
     */
    History("history"),

    /**
     * 回复类型的消息
     */
    Reply("reply");

    public String type;

    MessageType(String type) {
        this.type = type;
    }

    public static MessageType parse(String type) {
        for (MessageType messageType : values()) {
            if (messageType.type.equals(type)) {
                return messageType;
            }
        }
        return UnKnown;
    }
}
