package cube.contact.impl;

/**
 * 联系人模块的指令动作
 *
 * @author LiuFeng
 * @data 2020/9/1 17:43
 */
public interface ContactAction {

    /**
     * 账号登录
     */
    String signIn = "signIn";

    /**
     * 账号登出
     */
    String signOut = "signOut";

    /**
     * 获取指定联系人的信息。
     */
    String GetContact = "getContact";
}
