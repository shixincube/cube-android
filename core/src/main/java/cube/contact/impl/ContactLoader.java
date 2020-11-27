package cube.contact.impl;

import cube.contact.service.ContactService;
import cube.core.EngineAgent;

/**
 * 联系人服务加载者
 *
 * @author LiuFeng
 * @data 2020/9/1 18:30
 */
public class ContactLoader {

    /**
     * 静态代码块
     * 类加载时，将自动完成对服务的加载
     */
    static {
        EngineAgent.getInstance().loadService(ContactService.class, ContactServiceImpl.class);
    }
}
