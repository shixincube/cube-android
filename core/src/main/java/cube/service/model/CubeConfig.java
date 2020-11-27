package cube.service.model;

/**
 * 配置参数用于对引擎的运行状态及相关指标进行调整。 可调整的参数有视频控制参数、消息控制参数、白板控制参数等。
 *
 * @author workerinchina@163.com
 */
public final class CubeConfig {
    /**
     * AppKey
     */
    private String appKey;
    /**
     * AppId
     */
    private String appId;
    /**
     * 授权服务器地址
     */
    private String licenseServer;
    /**
     * 文件传输协议 1 HTTP
     */
    private int     transportProtocol = 1;
    /**
     * 是否支持SIP协议
     */
    private boolean isSupportSip      = true;
    /**
     * 是否保持在线
     */
    private boolean isAlwaysOnline    = true;

    /**
     * 是否需要收到自动登录回调
     */
    private boolean isAutoLoginCallBack   = false;
    /**
     * 是否自动重发失败消息
     */
    private boolean autoSendFailedMsg = true;
    /**
     * 是否在登录后自动同步消息
     */
    private boolean autoSyncMsg       = true;
    /**
     * 是否更新授权信息
     */
    private boolean isUpdateLicense   = true;
    /**
     * 是否为调试模式
     */
    private boolean isDebug           = false;
    /**
     * 应用数据存储目录
     */
    private String resourceDir;
    /**
     * 是否启用硬件解码
     */
    private boolean isHardwareDecoding = true;
    /**
     * 默认使用摄像头配置 1 前置 0 后置
     */
    private int     cameraId           = 1;
    /**
     * 音频编解码配置[OPUS,ISAC]
     */
    private String  audioCodec         = "OPUS";
    /**
     * 视频编解码配置[VP8,VP9,H264 High]
     */
    private String  videoCodec         = "VP9";
    /**
     * 视频宽度
     */
    private int     videoWidth         = 1280;
    /**
     * 视频高度
     */
    private int     videoHeight        = 720;
    /**
     * 视频帧率
     */
    private int     videoFps           = 18;
    /**
     * 最大视频宽带
     */
    private int     videoMaxBitrate    = 2000;//1M带宽即指1Mbps=1000Kbps=1000/8KBps=125KBps
    /**
     * 最小码率
     */
    private int     videoMinBitrate    = 300;//1M带宽即指1Mbps=1000Kbps=1000/8KBps=125KBps
    /**
     * 最大音频宽带
     */
    private int     audioMinBitrate    = 32;//1M带宽即指1Mbps=1000Kbps=1000/8KBps=125KBps

    /**
     * 是否支持H264
     */
    private boolean isSupportH264 = false;

    public CubeConfig() {}

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public int getTransportProtocol() {
        return transportProtocol;
    }

    public void setTransportProtocol(int transportProtocol) {
        this.transportProtocol = transportProtocol;
    }

    public boolean isSupportSip() {
        return isSupportSip;
    }

    public void setSupportSip(boolean supportSip) {
        isSupportSip = supportSip;
    }

    public boolean isAlwaysOnline() {
        return isAlwaysOnline;
    }

    public void setAlwaysOnline(boolean alwaysOnline) {
        isAlwaysOnline = alwaysOnline;
    }

    public boolean isAutoLoginCallBack() {
        return isAutoLoginCallBack;
    }

    public void setAutoLoginCallBack(boolean autoLoginCallBack) {
        isAutoLoginCallBack = autoLoginCallBack;
    }

    public boolean isAutoSendFailedMsg() {
        return autoSendFailedMsg;
    }

    public void setAutoSendFailedMsg(boolean autoSendFailedMsg) {
        this.autoSendFailedMsg = autoSendFailedMsg;
    }

    public boolean isAutoSyncMsg() {
        return autoSyncMsg;
    }

    public void setAutoSyncMsg(boolean autoSyncMsg) {
        this.autoSyncMsg = autoSyncMsg;
    }

    public boolean isUpdateLicense() {
        return isUpdateLicense;
    }

    public void setUpdateLicense(boolean updateLicense) {
        isUpdateLicense = updateLicense;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public void setDebug(boolean debug) {
        isDebug = debug;
    }

    public String getResourceDir() {
        return resourceDir;
    }

    public void setResourceDir(String resourceDir) {
        this.resourceDir = resourceDir;
    }

    public boolean isHardwareDecoding() {
        return isHardwareDecoding;
    }

    public void setHardwareDecoding(boolean hardwareDecoding) {
        isHardwareDecoding = hardwareDecoding;
    }

    public int getCameraId() {
        return cameraId;
    }

    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }

    public int getVideoFps() {
        return videoFps;
    }

    public void setVideoFps(int videoFps) {
        this.videoFps = videoFps;
    }

    public int getVideoMaxBitrate() {
        return videoMaxBitrate;
    }

    public void setVideoMaxBitrate(int videoMaxBitrate) {
        this.videoMaxBitrate = videoMaxBitrate;
    }

    public int getVideoMinBitrate() {
        return videoMinBitrate;
    }

    public void setVideoMinBitrate(int videoMinBitrate) {
        this.videoMinBitrate = videoMinBitrate;
    }

    public int getAudioMinBitrate() {
        return audioMinBitrate;
    }

    public void setAudioMinBitrate(int audioMinBitrate) {
        this.audioMinBitrate = audioMinBitrate;
    }

    public String getLicenseServer() {
        return licenseServer;
    }

    public void setLicenseServer(String licenseServer) {
        this.licenseServer = licenseServer;
    }

    public boolean isSupportH264() {
        return isSupportH264;
    }

    public void setSupportH264(boolean supportH264) {
        isSupportH264 = supportH264;
    }

}
