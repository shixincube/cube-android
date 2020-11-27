package cube.auth.impl;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import cube.auth.service.AuthService;
import cube.auth.service.AuthToken;
import cube.common.callback.Callback;
import cube.common.callback.CubeCallback1;
import cube.core.Module;
import cube.core.manager.CallBackManager;
import cube.pipeline.Packet;
import cube.pipeline.Pipeline;
import cube.pipeline.PipelineListener;
import cube.core.Service;
import cube.service.model.State;
import cube.utils.SpUtil;
import cube.utils.log.LogUtil;

/**
 * 授权服务实现
 *
 * @author LiuFeng
 * @data 2020/9/1 14:28
 */
public class AuthServiceImpl implements AuthService, Module, PipelineListener {
    private static final String TAG = "AuthServiceImpl";

    private Context mContext;
    private Pipeline pipeline;
    private AuthToken authToken;

    public AuthServiceImpl(Context context) {
        LogUtil.i(TAG, "AuthService:new");
        this.mContext = context;
    }

    @Override
    public String getName() {
        return AuthService.class.getSimpleName();
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
        pipeline.addListener(Service.AuthService, this);
    }

    @Override
    public void stop() {
        pipeline.removeListener(Service.AuthService, this);
    }

    @Override
    public AuthToken getToken() {
        return authToken;
    }

    @Override
    public void applyToken(String domain, String appKey, CubeCallback1<AuthToken> callback) {
        JSONObject param = new JSONObject();
        try {
            param.put("domain", domain);
            param.put("appKey", appKey);
        } catch (JSONException e) {
            LogUtil.e(TAG, e);
        }

        send(AuthAction.ApplyToken, param, callback);
    }

    @Override
    public void checkToken(String domain, String appKey, String address, CubeCallback1<AuthToken> callback) {
        // 尝试读取本地的 Token
        AuthToken token = SpUtil.getAuthToken();
        if (token == null) {
            applyToken(domain, appKey, callback);
            return;
        }

        // 判断令牌是否到有效期
        if (token.isValid()) {
            this.authToken = token;
            callback.onSuccess(authToken);
            return;
        }

        JSONObject param = new JSONObject();
        try {
            param.put("code", token.code);
        } catch (JSONException e) {
            LogUtil.e(TAG, e);
        }

        send(AuthAction.GetToken, param, callback);
    }

    @Override
    public void onReceived(String service, String action, State state, Packet packet) {
        switch (action) {
            case AuthAction.ApplyToken:
                this.processApplyToken(state, packet);
                break;

            case AuthAction.GetToken:
                this.processGetToken(state, packet);
                break;
        }
    }

    private void processGetToken(State state, Packet packet) {
        CubeCallback1<AuthToken> callback = CallBackManager.getCallBack(packet.sn);
        if (state.code == Service.StateOk) {
            authToken = AuthToken.create(packet.data);
            SpUtil.setAuthToken(authToken);
            callback.onSuccess(authToken);
        } else {
            callback.onError(state.code, state.desc);
        }
    }

    private void processApplyToken(State state, Packet packet) {
        CubeCallback1<AuthToken> callback = CallBackManager.getCallBack(packet.sn);
        if (state.code == Service.StateOk) {
            authToken = AuthToken.create(packet.data);
            SpUtil.setAuthToken(authToken);
            callback.onSuccess(authToken);
        } else {
            callback.onError(state.code, state.desc);
        }
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
        pipeline.send(Service.AuthService, packet);
    }
}
