package cube.message.impl;

import cube.core.EngineAgent;
import cube.message.service.MessageService;

/**
 * 消息服务加载者
 *
 * @author LiuFeng
 * @data 2020/9/3 15:01
 */
public class MessageLoader {

    /**
     * 静态代码块
     * 类加载时，将自动完成对服务的加载
     */
    static {
        EngineAgent.getInstance().loadService(MessageService.class, MessageServiceImpl.class);
    }
}
