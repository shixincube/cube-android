package cube.whiteboard.impl;

import cube.core.EngineAgent;
import cube.whiteboard.service.WhiteboardService;

/**
 * 白板服务加载者
 *
 * @author LiuFeng
 * @data 2019/3/26 9:40
 */
public class WhiteboardLoader {

    /**
     * 静态代码块
     * 类加载时，将自动完成对服务的加载
     */
    static {
        EngineAgent.getInstance().loadService(WhiteboardService.class, WhiteboardServiceImpl.class);
    }
}
