package cube.service.model;

/**
 * 错误码定义。
 *
 * @author workerinchina@163.com
 */
public enum CubeErrorCode {

    Unknown(0, "未知的错误"),
    OK(200, "成功"),
    InviteAccept(202, "正常接收"),
    BadRequest(400, "请求无效"),
    Unauthorized(401, "未授权请求"),
    PaymentRequired(402, "支付请求"),
    Forbidden(403, "服务器无法识别请求"),
    NotFound(404, "服务器没有找到对应的请求"),
    MethodNotAllowed(405, "请求指定的方法服务器不允许执行"),
    ProxyAuthenticationRequired(407, "代理需要授权"),
    RequestTimeout(408, "客户端的请求超时"),
    UnsupportedMediaType(415, "不支持的媒体类型"),
    RequestSendFailed(477, "请求数据发送失败"),
    TemporarilyUnavailable(480, "暂时不可用"),
    BusyHere(486, "对方正忙"),
    RequestTerminated(487, "对方未接听"),
    ServerInternalError(500, "服务器内部错误"),
    GatewayTimeout(504, "网关超时"),
    DoNotDisturb(600, "免打扰"),
    Declined(603, "拒绝请求"),
    ConnectionFailed(700, "连接失败"),
    SignalingStartError(701, "信令启动错误"),
    TransportError(702, "信令链路传输数据错误"),
    ICEConnectionFailed(703, "ICE连接失败"),
    CreateSessionDescriptionFailed(705, "创建SDP失败"),
    SetSessionDescriptionFailed(706, "设置SDP失败"),
    RTCInitializeFailed(707, "RTC初始化失败"),
    DuplicationException(710, "通话中，新呼叫进入"),
    WorkerStateException(711, "工作机状态异常"),
    ConferenceInitFailed(714, "会议初始化失败"),
    ConferenceNotExist(715, "会议不存在"),
    ConferenceKick(801, "会议踢出"),
    ConferenceConfigError(720, "会议配置错误"),
    CallTimeout(716, "呼叫超时"),
    AnswerTimeout(717, "应答超时"),
    TerminateTimeout(718, "挂断超时"),
    IncorrectState(801, "不正确的状态"),
    NetworkNotReachable(809, "网络不可达"),
    ContentTooLong(810, "内容长度越界"),
    BlockOversize(811, "消息缓存块大小超限"),
    FormatError(812, "消息格式错误"),
    ContentError(813, "消息内容错误"),
    OutOfLimit(814, "消息内容参数越限"),
    NoReceiver(815, "消息没有接收者"),
    RepeatMessage(816, "同一消息重复发送"),
    NullMessage(817, "空消息"),
    LostAssets(820, "丢失资源文件"),
    UploadFailed(823, "上传文件失败"),
    ProcessFailed(825, "处理文件失败"),
    LoadFileFailed(828, "文件加载失败"),
    OtherTerminalsAnswered(900, "其它终端接听"),
    OtherTerminalsCancel(901, "其它终端取消"),
    InvalidAccount(1001, "登录帐号超出授权错误"),
    RegisterTimeout(1002, "登录超时错误"),
    LicenseOutDate(1003, "授权过期"),
    UnRegister(1004, "未注册"),
    LicenseServerError(1005, "授权服务器错误"),
    LicenseUpdateError(1006, "授权服务器更新错误"),
    LicenseUpToDate(1007, "授权信息已是最新"),
    RecallError(1100, "撤回错误"),
    ForwardError(1101, "转发错误"),
    VoiceClipTooShort(1102, "语音录制太短"),
    VideoClipTooShort(1103, "视频录制太短"),
    MessageNotExist(1104, "消息不存在"),
    MessageTimeout(1105, "消息超时"),
    ReceiptError(1106, "消息回执错误"),
    QueryNOData(1107, "消息不存在"),
    DispatchFailed(1108, "消息分发失败"),
    OutDateMessage(1109, "过期的消息，消息超过5分钟不能撤回"),
    FileMessageDownloadFailed(1110, "文件下载失败"),
    FileMessageDownloadTimeout(1111, "文件消息下载超时"),
    FileMessageExist(1112, "文件消息已存在"),
    NoPullMessage(1113, "没有消息记录"),
    VoiceClipError(1114, "语音录制错误"),
    FileNotExistOnServer(1200, "服务器不存在此文件"),
    FileUploadError(1201, "云盘文件上传错误"),
    FileDataFormatError(1202, "云盘文件数据格式错误"),
    RenamedFailed(1203, "文件重命名失败失败"),
    CubeStateLoadFileFailed(1204, "文件加载失败"),
    DeleteFailed(1205, "文件删除失败"),
    MkdirFailed(1206, "创建文件夹失败"),
    FileUsedByOther(1207, "文件被占用"),
    GroupAddMasterFailed(1300, "添加管理员失败"),
    GroupRemoveMasterFailed(1301, "删除管理员失败"),
    GroupDestroy(1302, "群组已销毁"),
    GroupNotExist(1303, "群组不存在"),
    NotInGroup(1304, "不在群组中"),
    AlreadyInCalling(1400, "当前正在呼叫"),
    ConferenceExist(1500, "会议已存在"),
    OverMaxNumber(1501, "会议人数已达上限"),
    ConferenceRejectByOther(1502, "会议被其他端拒绝"),
    ConferenceJoinFromOther(1503, "您已在其他终端参会"),
    ApplyConferenceFailed(1504, "申请会议失败"),
    ConferenceJoinCancel(1505, "没有邀请人员"),
    ApplyJoinConferenceFailed(1506, "申请进入会议失败"),
    ConferenceClosed(1508, "会议已经销毁"),
    NotConferenceMaster(1511, "非会议管理员"),
    JoinConferenceEarly(1520, "加入会议太早"),
    ConferenceMemberStatusNotFound(1514, "会议成员状态不存在"),
    ExportWhiteboardFailed(1600, "导出白板错误"),
    ImportWhiteboardFailed(1601, "导入白板错误"),
    WhiteboardFailed(1602, "白板错误"),
    WhiteboardNotFound(400, "白板没找到"),
    CameraOpenFailed(1700, "摄像头开启失败"),
    ActiveAudioSessionFailed(1701, "激活音频失败"),
    MicphoneOpenFailed(1702, "麦克风开启失败"),
    VideoConvertFailed(1703, "录制视频转换失败"),
    AudioRecorderInitialFailed(1704, "音频录制初始化失败"),
    AudioRecorderPrepareFailed(1705, "音频准备录制失败"),
    AudioPlayDecodeFailed(1706, "解码出错"),
    NotSupportUserMedia(1707, "不支持操作用户媒体设备"),
    RemoteDeskTopExist(2000, "远程桌面已存在"),
    RemoteDeskTopNotExist(2001, "远程桌面不存在"),
    RemoteDeskTopUserAlreadyApply(2002, "用户已经申请加入了桌面分享"),
    RemoteDeskTopUserNotInvited(2003, "用户没有被邀请加入桌面分享"),
    RemoteDeskTopUserNeverApply(2004, "用户没有申请过桌面分享"),
    RemoteDeskTopUserNotContainThisGroup(2005, "该用户不属于该群组"),
    RemoteDeskTopDataStreamFailed(2006, "桌面数据流错误"),
    DeviceExist(2401,"设备已注册"),
    NotHavePermission(10000, "没有此操作的权限");

    public int code;
    public String message;

    /**
     * 构造法
     *
     * @param code
     * @param message
     */
    CubeErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据状态码查相应枚举状态
     *
     * @param stateCode
     *
     * @return
     */
    public static CubeErrorCode convert(int stateCode) {
        for (CubeErrorCode ec : CubeErrorCode.values()) {
            if (ec.code == stateCode) {
                return ec;
            }
        }
        return Unknown;
    }
}
