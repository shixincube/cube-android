package cube.contact.service;

import cube.common.callback.CubeCallback1;

/**
 * 联系人服务
 *
 * @author LiuFeng
 * @data 2020/9/1 18:09
 */
public interface ContactService {

    void setSelf(Self self, CubeCallback1<Contact> callback);

    Self getSelf();

    void updateSelf(Self self, CubeCallback1<Contact> callback);

    void getContact(long id, CubeCallback1<Contact> callback);
}
