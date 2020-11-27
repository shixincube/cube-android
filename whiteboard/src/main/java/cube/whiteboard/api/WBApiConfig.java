package cube.whiteboard.api;

import cube.whiteboard.api.data.LicenseData;

/**
 * 白板api配置数据
 *
 * @author LiuFeng
 * @data 2020/8/24 18:12
 */
public class WBApiConfig {
    private static LicenseData license = new LicenseData();

    public static void setLicense(LicenseData data) {
        license = data;
    }

    public static String getHost() {
        return license.host;
    }

    public static int getTcpPort() {
        return license.tcpPort;
    }

    public static int getHttpPort() {
        return license.httpPort;
    }

    /**
     * 获取白板的BaseUrl
     *
     * @return
     */
    public static String getBaseUrl() {
        String protocol = license.secure ? "https" : "http";
        return protocol + "://" + getHost() + ":" + getHttpPort();
    }
}
