package cube.auth.service;

import cube.common.callback.CubeCallback1;

/**
 * 授权信息
 * 描述：管理引擎的授权信息。
 *
 * @author LiuFeng
 * @data 2020/9/1 14:18
 */
public interface AuthService {

    /**
     * 获取令牌实例。
     *
     * @return
     */
    AuthToken getToken();

    /**
     * 申请令牌。
     *
     * @param domain
     * @param appKey
     * @param callback
     */
    void applyToken(String domain, String appKey, CubeCallback1<AuthToken> callback);

    /**
     * 校验当前的令牌是否有效。
     * 该方法先从本地获取本地令牌进行校验，如果本地令牌失效或者未找到本地令牌，则尝试从授权服务器获取有效的令牌。
     *
     * @param domain    令牌对应的域。
     * @param appKey    令牌指定的 App Key 串。
     * @param address   授权服务器地址。
     * @param callback
     */
    void checkToken(String domain, String appKey, String address, CubeCallback1<AuthToken> callback);
}
