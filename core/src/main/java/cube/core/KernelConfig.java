package cube.core;

/**
 * 内核配置
 *
 * @author LiuFeng
 * @data 2020/10/9 16:52
 */
public class KernelConfig {
    public int port;        // 管道服务器端口号。
    public String address;  // 管道服务器地址。
    public String domain;   // 授权的指定域。
    public String appKey;   // 当前应用申请到的 App Key 串。
}
