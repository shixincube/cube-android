package cube.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import cube.auth.service.AuthToken;
import cube.common.CubeConstants;

import java.io.File;

/**
 * SharedPreferences工具类
 *
 * @author LiuFeng
 * @date 2018-6-9
 */
public class SpUtil {
    private static SharedPreferences sp;
    private static Context           mContext;

    public static void init(Context context) {
        if (sp == null) {
            mContext = context;
            sp = context.getSharedPreferences("cube_engine_preferences", Context.MODE_PRIVATE);
        }
    }

    /**
     * 初始化用户
     *
     * @param cubeId
     * @param displayName
     * @param token
     */
    public static void initUser(@NonNull String cubeId, @NonNull String displayName, @NonNull AuthToken token) {
        setCubeId(cubeId);
        setDisplayName(displayName);
        setAuthToken(token);
    }

    /**
     * 是否初始化用户
     *
     * @return
     */
    public static boolean isInitUser() {
        return contains(CubeConstants.Sp.CUBE_ID) && contains(CubeConstants.Sp.DISPLAY_NAME) && contains(CubeConstants.Sp.CUBE_TOKEN);
    }

    /**
     * 获取用户cubeId
     *
     * @return
     */
    public static String getCubeId() {
        return getString(CubeConstants.Sp.CUBE_ID, "");
    }

    public static void setCubeId(@NonNull String cubeId) {
        setString(CubeConstants.Sp.CUBE_ID, cubeId);
    }

    /**
     * 获取用户显示名称
     *
     * @return
     */
    public static String getDisplayName() {
        return getString(CubeConstants.Sp.DISPLAY_NAME, "");
    }

    public static void setDisplayName(@NonNull String displayName) {
        setString(CubeConstants.Sp.DISPLAY_NAME, displayName);
    }

    public static String getAvatar() {
        return getString(CubeConstants.Sp.AVATAR, "");
    }

    public static void setAvatar(@NonNull String avatar) {
        setString(CubeConstants.Sp.AVATAR, avatar);
    }

    /**
     * 获取用户AuthToken
     *
     * @return
     */
    public static AuthToken getAuthToken() {
        String tokenJson = getString(CubeConstants.Sp.CUBE_TOKEN, null);
        if (tokenJson != null) {
            return GsonUtil.toBean(tokenJson, AuthToken.class);
        }

        return null;
    }

    /**
     * 设置用户AuthToken
     *
     * @param token
     */
    public static void setAuthToken(@NonNull AuthToken token) {
        setString(CubeConstants.Sp.CUBE_TOKEN, GsonUtil.toJson(token));
    }

    /**
     * 删除用户
     */
    public static void removeUser() {
        remove(CubeConstants.Sp.CUBE_ID);
        remove(CubeConstants.Sp.DISPLAY_NAME);
        remove(CubeConstants.Sp.CUBE_TOKEN);
    }

    /**
     * 设置LicenseServer
     *
     * @param server
     */
    public static void setLicenseServer(@NonNull String server) {
        setString(CubeConstants.Sp.LICENSE_SERVER, server);
    }

    /**
     * 获取LicenseServer
     *
     * @return
     */
    public static String getLicenseServer() {
        String licenseServer = CubeConstants.Release.LICENSE_URL; // 默认地址：正式地址
        //String licenseServer = CubeConstants.Debug.LICENSE_URL;   // 默认地址：测试地址
        return getString(CubeConstants.Sp.LICENSE_SERVER, licenseServer);
    }

    /**
     * 删除LicenseServer
     */
    public static void removeLicenseServer() {
        remove(CubeConstants.Sp.LICENSE_SERVER);
    }

    public static void setLicense(String license) {
        setString(CubeConstants.Sp.LICENSE_STRING, license);
    }

    public static String getLicense() {
        return getString(CubeConstants.Sp.LICENSE_STRING, "");
    }

    public static void setLicenseChange(boolean isLicenseChange) {
        setBoolean(CubeConstants.Sp.IS_LICENSE_CHANGE, isLicenseChange);
    }

    public static boolean isLicenseChange() {
        return getBoolean(CubeConstants.Sp.IS_LICENSE_CHANGE, true);
    }

    public static void setResourcePath(String dir) {
        setString(CubeConstants.Sp.PATH_RESOURCE, dir);
    }

    public static String getResourcePath() {
        String path = getString(CubeConstants.Sp.PATH_RESOURCE, null);
        if (TextUtils.isEmpty(path)) {
            setString(CubeConstants.Sp.PATH_RESOURCE, getAppFilesDir());
        }
        path = getString(CubeConstants.Sp.PATH_RESOURCE, null);
        CubeInitUtil.initFileDir(new File(path));
        return path;
    }

    public static String getAppFilesDir() {
        File dir = new File(mContext.getFilesDir().getAbsolutePath() + File.separator + CubeConstants.Sp.PATH_DIR_DEF);
        FileUtil.createOrExistsDir(dir);
        return dir.getAbsolutePath();
    }

    public static String getVoiceResourcePath() {
        return getResourcePath() + File.separator + CubeConstants.Sp.PATH_VOICE_DEF;
    }

    public static String getVideoResourcePath() {
        return getResourcePath() + File.separator + CubeConstants.Sp.PATH_VIDEO_DEF;
    }

    public static String getFileResourcePath() {
        return getResourcePath() + File.separator + CubeConstants.Sp.PATH_FILE_DEF;
    }

    public static String getImageResourcePath() {
        return getResourcePath() + File.separator + CubeConstants.Sp.PATH_IMAGE_DEF;
    }

    public static String getFileYunResourcePath() {
        return getResourcePath() + File.separator + CubeConstants.Sp.PATH_FILE_YUN_DEF;
    }

    public static String getWBResourcePath() {
        return getResourcePath() + File.separator + CubeConstants.Sp.PATH_FILE_WB_DEF;
    }

    public static String getTmpFileResourcePath() {
        return getResourcePath() + File.separator + CubeConstants.Sp.PATH_TMP_DEF;
    }

    public static String getThumbResourcePath() {
        return getResourcePath() + File.separator + CubeConstants.Sp.PATH_THUMB_DEF;
    }

    public static String getLogResourcePath() {
        return getResourcePath() + File.separator + CubeConstants.Sp.PATH_LOG_DEF;
    }

    public static int getVersionWB() {
        return sp.getInt(CubeConstants.Sp.WB_VERSION, 0);
    }

    public static void setVersionWB(int versionCode) {
        setInt(CubeConstants.Sp.WB_VERSION, versionCode);
    }

    public static int getTransportProtocol() {
        return getInt(CubeConstants.Sp.TRANSPORT_PROTOCOL, 1);
    }

    public static void setTransportProtocol(int transportProtocol) {
        setInt(CubeConstants.Sp.TRANSPORT_PROTOCOL, transportProtocol);
    }

    public static boolean isSupportSip() {
        return getBoolean(CubeConstants.Sp.SUPPORT_SIP, false);
    }

    public static void setSupportSip(boolean isSupportSip) {
        setBoolean(CubeConstants.Sp.SUPPORT_SIP, isSupportSip);
    }

    public static File getLicenseFile() {
        return new File(mContext.getFilesDir() + File.separator + CubeConstants.Sp.LICENCE);
    }

    public static String getAudioCodec() {
        return getString(CubeConstants.Sp.AUDIO_CODEC, "opus");
    }

    public static void setAudioCodec(String audioCodec) {
        setString(CubeConstants.Sp.AUDIO_CODEC, audioCodec);
    }

    public static String getVideoCodec() {
        return getString(CubeConstants.Sp.VIDEO_CODEC, "VP8");
    }

    public static void setVideoCodec(String videoCodec) {
        setString(CubeConstants.Sp.VIDEO_CODEC, videoCodec);
    }

    public static int getVideoId() {
        return getInt(CubeConstants.Sp.VIDEO_ID, 0);
    }

    public static void setVideoId(int videoId) {
        setInt(CubeConstants.Sp.VIDEO_ID, videoId);
    }

    public static void setCertCrtPath(String crt) {
        setString(CubeConstants.Sp.CRT_PATH, crt);
    }

    public static String getCertCrtPath() {
        return getString(CubeConstants.Sp.CRT_PATH, "");
    }

    public static void setCertKeyPath(String key) {
        setString(CubeConstants.Sp.KEY_PATH, key);
    }

    public static String getCertKeyPath() {
        return sp.getString(CubeConstants.Sp.KEY_PATH, "");
    }

    public static boolean hasCert() {
        String crtPath = getString(CubeConstants.Sp.CRT_PATH, null);
        String keyPath = getString(CubeConstants.Sp.KEY_PATH, null);
        if (!TextUtils.isEmpty(crtPath) && !TextUtils.isEmpty(keyPath)) {
            File crt = new File(crtPath);
            File key = new File(keyPath);
            return crt.exists() && key.exists();
        }
        return false;
    }

    public static void setLocation(String location) {
        setString(CubeConstants.Sp.LOCATION, location);
    }

    public static String getLocation() {
        String locationJson = getString(CubeConstants.Sp.LOCATION, "");
        return locationJson;
    }

    public static void setMsgToken(String msgToken) {
        setString(CubeConstants.Sp.MSG_TOKEN, msgToken);
    }

    public static String getMsgToken() {
        return getString(CubeConstants.Sp.MSG_TOKEN, "100001");
    }

    public static void setDeviceId(String deviceId) {
        setString(CubeConstants.Sp.DEVICE_ID + getCubeId(), deviceId);
    }

    public static String getDeviceId() {
        return getString(CubeConstants.Sp.DEVICE_ID + getCubeId(), "");
    }

    /**
     * 设置基础的DBName
     *
     * @param dbName
     */
    public static void setBaseDBName(@NonNull String dbName) {
        setString(CubeConstants.Sp.BASE_DB_NAME, dbName);
    }

    public static String getBaseDBName() {
        return getString(CubeConstants.Sp.BASE_DB_NAME, null);
    }

    /**
     * 设置文件的DBName
     *
     * @param dbName
     */
    public static void setFileDBName(@NonNull String dbName) {
        setString(CubeConstants.Sp.FILE_DB_NAME, dbName);
    }

    public static String getFileDBName() {
        return getString(CubeConstants.Sp.FILE_DB_NAME, null);
    }

    /**
     * 设置消息的DBName
     *
     * @param dbName
     */
    public static void setMessageDBName(@NonNull String dbName) {
        setString(CubeConstants.Sp.MESSAGE_DB_NAME, dbName);
    }

    public static String getMessageDBName() {
        return getString(CubeConstants.Sp.MESSAGE_DB_NAME, null);
    }

    /**
     * 保存最近设置信息
     *
     * @param setting
     */
    public static void setSetting(String setting) {
        setString(CubeConstants.Sp.SETTING, setting);
    }

    /**
     * 获取最近设置信息
     *
     * @return
     */
    public static String getSetting() {
        return getString(CubeConstants.Sp.SETTING, "");
    }

    public static void setLicenseUpdateTime(long updateTime) {
        setLong(CubeConstants.Sp.LICENSE_UPDATE_TIME, updateTime);
    }

    public static long getLicenseUpdateTime() {
        return getLong(CubeConstants.Sp.LICENSE_UPDATE_TIME, 0);
    }

    public static void setICEServer(String ice) {
        setString(CubeConstants.Sp.ICE_SERVER, ice);
    }

    public static String getICEServerToString() {
        return getString(CubeConstants.Sp.ICE_SERVER, "{}");
    }

    public static String getICEServer() {
        String iceJson = getString(CubeConstants.Sp.ICE_SERVER, "{}");
        return iceJson;
    }

    public static void setOtherICEServer(String ice) {
        setString(CubeConstants.Sp.OTHER_ICE_SERVER, ice);
    }

    public static String getOtherICEServer() {
        return getString(CubeConstants.Sp.OTHER_ICE_SERVER, "{}");
    }

    public static void clearOtherICEServer() {
        remove(CubeConstants.Sp.OTHER_ICE_SERVER);
    }

    public static void setTag(String tag) {
        setString(CubeConstants.Sp.LOGIN_TAG, tag);
    }

    public static String getTag() {
        return getString(CubeConstants.Sp.LOGIN_TAG, "");
    }

    public static boolean isIPChanged(String ip) {
        boolean isChanged = getString("ip", "").equals(ip);
        if (isChanged) {
            setString("ip", ip);
        }
        return isChanged;
    }

    public static void setLastLoginTimestamp(long time) {
        setLong("LastLoginTimestamp" + getCubeId(), time);
    }

    public static long getOffsetTime() {
        return getLong("offsetTime", 0L);
    }

    public static void setOffsetTime(long offsetTime) {
        setLong("offsetTime", offsetTime);
    }

    /**
     * 调试设置
     *
     * @param debug
     */
    public static void setDebug(boolean debug) {
        setBoolean(CubeConstants.Sp.DEBUG, debug);
    }

    /**
     * 获取是否调试
     */
    public static boolean isDebug() {
        return getBoolean(CubeConstants.Sp.DEBUG, false);
    }

    //==============================sp基础方法，用于封装具体方法=============================//

    /**
     * 获取String--基础方法
     */
    private static String getString(@NonNull String key, String defValue) {
        return sp.getString(key, defValue);
    }

    /**
     * 设置String--基础方法
     */
    private static void setString(@NonNull String key, String value) {
        sp.edit().putString(key, value).apply();
    }

    /**
     * 获取Boolean--基础方法
     */
    private static boolean getBoolean(@NonNull String key, boolean defValue) {
        return sp.getBoolean(key, defValue);
    }

    /**
     * 设置Boolean--基础方法
     */
    private static void setBoolean(@NonNull String key, boolean value) {
        sp.edit().putBoolean(key, value).apply();
    }

    /**
     * 获取Int--基础方法
     */
    private static int getInt(@NonNull String key, int defValue) {
        return sp.getInt(key, defValue);
    }

    /**
     * 设置Int--基础方法
     */
    private static void setInt(@NonNull String key, int value) {
        sp.edit().putInt(key, value).apply();
    }

    /**
     * 获取Long--基础方法
     */
    private static long getLong(@NonNull String key, long defValue) {
        return sp.getLong(key, defValue);
    }

    /**
     * 设置Long--基础方法
     */
    private static void setLong(@NonNull String key, long value) {
        sp.edit().putLong(key, value).apply();
    }

    /**
     * 包含key键--基础方法
     */
    private static boolean contains(@NonNull String key) {
        return sp.contains(key);
    }

    /**
     * 移除指定key--基础方法
     */
    private static void remove(@NonNull String key) {
        sp.edit().remove(key).apply();
    }
}
