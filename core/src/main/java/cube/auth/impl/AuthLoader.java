package cube.auth.impl;

import cube.auth.service.AuthService;
import cube.core.EngineAgent;

/**
 * 授权服务加载者
 *
 * @author LiuFeng
 * @data 2020/9/1 15:46
 */
public class AuthLoader {

    /**
     * 静态代码块
     * 类加载时，将自动完成对服务的加载
     */
    static {
        EngineAgent.getInstance().loadService(AuthService.class, AuthServiceImpl.class);
    }
}
