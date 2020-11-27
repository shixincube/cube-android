package cube.core;

/**
 * 通信服务节点名称
 */
public interface Service {
    int StateOk = 1000;            // 正确响应状态码

    String SN = "sn";             // 指令sn标记KEY
    String PARAM = "param";       // 请求参数KEY
    String STATE = "state";       // 响应状态KEY
    String DATA = "data";         // 数据参数KEY
    String CODE = "code";         // 状态参数KEY
    String DESC = "desc";         // 描述参数KEY

    String AuthService = "Auth";                // 授权
    String ContactService = "Contact";          // 联系人
    String MessageService = "Messaging";          // 消息
    String WhiteboardService = "whiteboard";    // 白板
}
