package cube.common;

/**
 * 全局常量池类
 *
 * @author LiuFeng
 * @date 2018-5-26
 */
public class CubeConstants {

    /**
     * 正式服环境
     */
    public interface Release {
        String BASE_URL        = "http://getcube.cn";                                    // 服务器基础地址
        String USER_CENTER_URL = "https://aws-user.shixincube.com";                      // 用户中心基础地址
        String LICENSE_URL     = "https://aws-license.shixincube.com/auth/license/get";  // 授权接口地址
    }

    /**
     * 测试服环境
     */
    public interface Debug {
        String USER_CENTER_URL = "https://test-user.shixincube.cn";                      // 用户中心基础地址
        String LICENSE_URL     = "https://test-license.shixincube.cn/auth/license/get";  // 授权接口地址
    }

    /**
     * 接口服务
     */
    public interface ApiService {
        String API_TAG        = "api_tag";
        String FILE           = "file";
        String STORAGE        = "storage";
        String FILE_HEADER    = API_TAG + ":" + FILE;
        String STORAGE_HEADER = API_TAG + ":" + STORAGE;
    }

    /**
     * SharedPreferences常量
     */
    public interface Sp {
        String CUBE_ID         = "cube_id";
        String DISPLAY_NAME    = "display_name";
        String AVATAR          = "avatar";
        String CUBE_TOKEN      = "cube_token";
        String MSG_TOKEN       = "msg_token";
        String DEVICE_ID       = "device_id";
        String LOCATION        = "location";
        String BASE_DB_NAME    = "base_db_name";
        String FILE_DB_NAME    = "file_db_name";
        String MESSAGE_DB_NAME = "message_db_name";
        String SETTING         = "cube_setting";

        String PATH_DIR_DEF      = "cube";
        String PATH_VOICE_DEF    = "voice";
        String PATH_VIDEO_DEF    = "video";
        String PATH_FILE_DEF     = "file";
        String PATH_IMAGE_DEF    = "image";
        String PATH_FILE_YUN_DEF = "fileyun";
        String PATH_FILE_WB_DEF  = "wb";
        String PATH_LOG_DEF      = "engine_log";
        String PATH_TMP_DEF      = ".tmp";
        String PATH_THUMB_DEF    = ".thumb";

        String LICENCE             = "cube.impl.license";
        String LICENSE_STRING      = "license_string";
        String IS_LICENSE_CHANGE   = "is_license_change";
        String LICENSE_SERVER      = "license_server";
        String LICENSE_UPDATE_TIME = "license_update_time";
        String ICE_SERVER          = "server_ice";
        String OTHER_ICE_SERVER    = "other_server_ice";
        String PATH_RESOURCE       = "resource_path";
        String WB_VERSION          = "wb_version";
        String TRANSPORT_PROTOCOL  = "transport_protocol";
        String SUPPORT_SIP         = "is_support_sip";
        String CRT_PATH            = "crtPath";
        String KEY_PATH            = "keyPath";
        String VIDEO_CODEC         = "video_codec";
        String AUDIO_CODEC         = "audio_codec";
        String VIDEO_ID            = "video_id";
        String LOGIN_TAG           = "login_tag";
        String DEBUG               = "debug";
    }
}
