package cube.core;

/**
 * 服务加载者类路径
 *
 * @author LiuFeng
 * @data 2020/9/1 10:08
 */
public interface Loader {

    /**
     * 授权
     */
    String AuthLoader = "cube.auth.impl.AuthLoader";

    /**
     * 联系人
     */
    String ContactLoader = "cube.contact.impl.ContactLoader";

    /**
     * 消息
     */
    String MessageLoader = "cube.message.impl.MessageLoader";

    /**
     * 白板
     */
    String WhiteboardLoader = "cube.whiteboard.impl.WhiteboardLoader";
}
