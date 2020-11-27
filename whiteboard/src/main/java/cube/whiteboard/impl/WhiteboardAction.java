package cube.whiteboard.impl;

/**
 * 白板服务指令
 *
 * @author LiuFeng
 * @data 2020/8/17 10:55
 */
public interface WhiteboardAction {
    String JOIN = "join";
    String JOIN_ACK = "join-ack";

    String LEAVE = "leave";
    String LEAVE_ACK = "leave-ack";

    String WB = "wb";
    String WB_ACK = "wb-ack";
}