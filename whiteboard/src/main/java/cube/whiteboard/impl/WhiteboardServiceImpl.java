package cube.whiteboard.impl;

import android.content.Context;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cube.common.callback.Callback;
import cube.common.callback.CubeCallback0;
import cube.common.callback.CubeCallback1;
import cube.common.callback.CubeCallbackUI1;
import cube.core.Module;
import cube.core.manager.CallBackManager;
import cube.pipeline.Packet;
import cube.pipeline.Pipeline;
import cube.pipeline.PipelineListener;
import cube.core.Service;
import cube.service.model.State;
import cube.utils.GsonUtil;
import cube.utils.SpUtil;
import cube.utils.log.LogUtil;
import cube.whiteboard.api.WBApiConfig;
import cube.whiteboard.api.WBApiFactory;
import cube.whiteboard.api.data.HistoryData;
import cube.whiteboard.api.data.JoinWbData;
import cube.whiteboard.api.data.LeaveWbData;
import cube.whiteboard.api.data.LicenseData;
import cube.whiteboard.api.data.QueryFileData;
import cube.whiteboard.api.data.UpdateFileData;
import cube.whiteboard.service.Whiteboard;
import cube.whiteboard.service.WhiteboardListener;
import cube.whiteboard.service.WhiteboardService;
import cube.whiteboard.service.WhiteboardView;

/**
 * 白板服务实现
 *
 * @author LiuFeng
 * @data 2020/8/14 18:05
 */
public class WhiteboardServiceImpl implements WhiteboardService, Module, PipelineListener, WBJsListener {
    private static final String TAG = "WhiteboardServiceImpl";

    private Context mContext;
    private Pipeline pipeline;
    private String whiteboardId;
    private boolean isAutoSyncData;
    private WhiteboardViewEntity view;

    private List<WhiteboardListener> listeners = new ArrayList<>();
    private Map<String, Whiteboard> whiteboardMap = new ConcurrentHashMap<>();

    public WhiteboardServiceImpl(Context context) {
        LogUtil.i(TAG, "WhiteboardService:new");
        this.mContext = context;
        view = new WhiteboardViewEntity(mContext);
        view.setListener(this);
    }

    @Override
    public String getName() {
        return WhiteboardService.class.getSimpleName();
    }

    @Override
    public List<String> getRequires() {
        return null;
    }

    @Override
    public List<String> getRequireFiles() {
        return null;
    }

    @Override
    public void bindPipeline(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public void start() {
        pipeline.addListener(Service.WhiteboardService, this);
    }

    @Override
    public void stop() {
        pipeline.removeListener(Service.WhiteboardService, this);
    }

    @Override
    public void addListener(WhiteboardListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(WhiteboardListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void init(String token, CubeCallback0 callback) {
        try {
            // 解码token
            String decodeToken = new String(Base64.decode(token, Base64.DEFAULT), "UTF-8");
            LicenseData licenseData = GsonUtil.toBean(decodeToken, LicenseData.class);
            WBApiConfig.setLicense(licenseData);

            // 连接服务器
            pipeline.setRemoteAddress(licenseData.host, licenseData.tcpPort);
            pipeline.open();

            if (callback != null) {
                callback.onSuccess();
            }
        } catch (UnsupportedEncodingException e) {
            if (callback != null) {
                callback.onError(1002, e.getMessage());
            }
        }
    }

    @Override
    public void syncData(long startTime, CubeCallback0 callback) {
        WBApiFactory.getInstance().queryHistory(whiteboardId, "", new CubeCallback1<HistoryData>() {
            @Override
            public void onSuccess(HistoryData result) {
                List<HistoryData.Drawing> drawings = result.drawings;
                if (drawings != null && !drawings.isEmpty()) {
                    JSONArray array = new JSONArray();
                    for (HistoryData.Drawing drawing : drawings) {
                        if (drawing.reset == 1 && drawing.del <= 1) {
                            array.put(GsonUtil.toJSONObject(drawing.data));
                        }
                    }
                    view.loadCommand(array);
                }

                if (callback != null) {
                    callback.onSuccess();
                }

                /*for (WhiteboardListener listener : listeners) {
                    listener.onSyncDataCompleted();
                }*/
            }

            @Override
            public void onError(int code, String desc) {
                if (callback != null) {
                    callback.onError(code, desc);
                }
            }
        });
    }

    @Override
    public void join(String wid, CubeCallback1<WhiteboardView> callback) {
        this.whiteboardId = wid;
        view.setWhiteboardId(wid);
        Whiteboard whiteboard = new Whiteboard(wid);
        whiteboardMap.put(wid, whiteboard);

        JSONObject param = new JSONObject();
        try {
            param.put("roomId", wid);
            param.put("platform", "1");
            param.put("role", "presenter");
            param.put("uid", SpUtil.getCubeId());
            param.put("name", SpUtil.getDisplayName());
            param.put("avatar", SpUtil.getAvatar());
        } catch (JSONException e) {
            LogUtil.e(TAG, e);
        }

        send(WhiteboardAction.JOIN, param, new CubeCallbackUI1<>(callback));
    }

    @Override
    public void leave(String wid, CubeCallback1<Whiteboard> callback) {
        JSONObject param = new JSONObject();
        try {
            param.put("roomId", wid);
            param.put("uid", SpUtil.getCubeId());
        } catch (JSONException e) {
            LogUtil.e(TAG, e);
        }

        send(WhiteboardAction.LEAVE, param, new CubeCallbackUI1<>(callback));
    }

    public void sendCommand(String wid, JSONObject cmd, CubeCallback0 callback) {
        JSONObject param = new JSONObject();
        try {
            param.put("roomId", wid);
            param.put("uid", SpUtil.getCubeId());
            param.put("data", cmd);
        } catch (JSONException e) {
            LogUtil.e(TAG, e);
        }

        send(WhiteboardAction.WB, param, callback);
    }

    @Override
    public WhiteboardView getView() {
        return view;
    }

    @Override
    public void setAutoSyncData(boolean auto) {
        this.isAutoSyncData = auto;
    }

    @Override
    public boolean isAutoSyncData() {
        return isAutoSyncData;
    }

    @Override
    public void upload(String wid, File file, CubeCallback1<WhiteboardFile> callback) {
        WBApiFactory.getInstance().uploadFile(wid, "", file, new CubeCallback1<UpdateFileData>() {
            @Override
            public void onSuccess(UpdateFileData result) {
                if (callback != null) {
                    WhiteboardFile whiteboardFile = result.file;
                    whiteboardFile.file = file;
                    callback.onSuccess(whiteboardFile);
                }
            }

            @Override
            public void onError(int code, String desc) {
                if (callback != null) {
                    callback.onError(code, desc);
                }
            }
        });
    }

    @Override
    public void listFiles(String wid, CubeCallback1<List<WhiteboardFile>> callback) {
        WBApiFactory.getInstance().queryFiles(wid, "", new CubeCallback1<QueryFileData>() {
            @Override
            public void onSuccess(QueryFileData result) {
                if (callback != null) {
                    List<WhiteboardFile> files = result.files;
                    callback.onSuccess(files);
                }
            }

            @Override
            public void onError(int code, String desc) {
                if (callback != null) {
                    callback.onError(code, desc);
                }
            }
        });
    }

    @Override
    public void onReceived(String service, String action, State state, Packet packet) {
        switch (action) {
            case WhiteboardAction.JOIN:
                this.processJoin(state, packet);
                break;

            case WhiteboardAction.JOIN_ACK:
                this.processJoinAck(state, packet);
                break;

            case WhiteboardAction.LEAVE:
                this.processLeave(state, packet);
                break;

            case WhiteboardAction.LEAVE_ACK:
                this.processLeaveAck(state, packet);
                break;

            case WhiteboardAction.WB:
                this.processWb(state, packet);
                break;

            case WhiteboardAction.WB_ACK:
                this.processWbAck(state, packet);
                break;
        }
    }

    private void processJoin(State state, Packet packet) {
        if (state.code == Service.StateOk) {
            JoinWbData wbData = GsonUtil.toBean(packet.data, JoinWbData.class);
            for (WhiteboardListener listener : listeners) {
                listener.onJoined(wbData.roomId, wbData.uId);
            }
        }
    }

    private void processJoinAck(State state, Packet packet) {
        JoinWbData wbData = GsonUtil.toBean(packet.data, JoinWbData.class);
        CubeCallback1<WhiteboardView> callback = CallBackManager.getCallBack(packet.sn);

        if (state.code == Service.StateOk) {
            callback.onSuccess(view);
        } else {
            callback.onError(state.code, state.desc);
        }
    }

    private void processLeave(State state, Packet packet) {
        if (state.code == Service.StateOk) {
            LeaveWbData wbData = GsonUtil.toBean(packet.data, LeaveWbData.class);
            for (WhiteboardListener listener : listeners) {
                listener.onLeaved(wbData.roomId, wbData.uId);
            }
        }
    }

    private void processLeaveAck(State state, Packet packet) {
        LeaveWbData wbData = GsonUtil.toBean(packet.data, LeaveWbData.class);
        CubeCallback1<Whiteboard> callback = CallBackManager.getCallBack(packet.sn);
        if (state.code == Service.StateOk) {
            callback.onSuccess(whiteboardMap.get(wbData.actionId));
        } else {
            callback.onError(state.code, state.desc);
        }

        view.destroy();
        whiteboardMap.remove(wbData.actionId);
    }

    private void processWb(State state, Packet packet) {
        if (view == null) {
            return;
        }

        try {
            JSONObject data = packet.data;
            String whiteboardId = data.getString("roomId");
            if (whiteboardMap.containsKey(whiteboardId)) {
                JSONObject command = data.getJSONObject("data");

                JSONArray array = new JSONArray();
                array.put(command);
                view.loadCommand(array);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void processWbAck(State state, Packet packet) {

    }

    @Override
    public void onReady(WhiteboardView view) {
        for (WhiteboardListener listener : listeners) {
            listener.onReady();
        }

        if (isAutoSyncData) {
            syncData(0, null);
        }
    }

    @Override
    public void onDataChange(String wid, JSONObject cmd) {
        sendCommand(wid, cmd, null);
    }

    /**
     * 发送数据
     *
     * @param action
     * @param param
     * @param callback
     */
    private void send(String action, JSONObject param, Callback callback) {
        long sn = CallBackManager.addCallBack(callback);
        Packet packet = new Packet(sn, action, param);
        pipeline.send(Service.WhiteboardService, packet);
    }
}
