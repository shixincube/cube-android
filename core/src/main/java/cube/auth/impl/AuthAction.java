package cube.auth.impl;

/**
 * 授权模块的指令动作
 *
 * @author LiuFeng
 * @data 2020/9/1 13:59
 */
public interface AuthAction {

    /**
     * 申请Token
     */
    String ApplyToken = "applyToken";

    /**
     * 获取Token
     */
    String GetToken = "getToken";
}
