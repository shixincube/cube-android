package cube.contact.impl;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import cube.auth.service.AuthService;
import cube.auth.service.AuthToken;
import cube.common.callback.Callback;
import cube.common.callback.CubeCallback1;
import cube.common.callback.CubeCallbackUI1;
import cube.contact.service.Contact;
import cube.contact.service.ContactService;
import cube.contact.service.Self;
import cube.core.EngineAgent;
import cube.core.Module;
import cube.core.manager.CallBackManager;
import cube.pipeline.Packet;
import cube.pipeline.Pipeline;
import cube.pipeline.PipelineListener;
import cube.core.Service;
import cube.service.model.State;
import cube.utils.GsonUtil;
import cube.utils.log.LogUtil;

/**
 * 联系人服务实现
 *
 * @author LiuFeng
 * @data 2020/9/1 18:25
 */
public class ContactServiceImpl implements ContactService, Module, PipelineListener {
    private static final String TAG = "ContactServiceImpl";

    private Context mContext;
    private Pipeline pipeline;
    private AuthService authService;

    private Self self;

    public ContactServiceImpl(Context context) {
        LogUtil.i(TAG, "ContactService:new");
        this.mContext = context;
    }

    @Override
    public String getName() {
        return ContactService.class.getSimpleName();
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
        pipeline.addListener(Service.ContactService, this);
        authService = EngineAgent.getInstance().getService(AuthService.class);
    }

    @Override
    public void stop() {
        pipeline.removeListener(Service.ContactService, this);
    }

    @Override
    public void setSelf(Self self, CubeCallback1<Contact> callback) {
        this.self = self;
        JSONObject params = new JSONObject();
        try {
            params.put("self", self.toJSON());
            AuthToken token = authService.getToken();
            if (token != null) {
                params.put("token", GsonUtil.toJSONObject(token));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        send(ContactAction.signIn, params, new CubeCallbackUI1<>(callback));
    }

    @Override
    public Self getSelf() {
        return self;
    }

    @Override
    public void updateSelf(Self self, CubeCallback1<Contact> callback) {
        setSelf(self, callback);
    }

    @Override
    public void getContact(long id, CubeCallback1<Contact> callback) {
        JSONObject param = new JSONObject();
        try {
            param.put("id", id);
        } catch (JSONException e) {
            LogUtil.e(TAG, e);
        }

        send(ContactAction.GetContact, param, callback);
    }

    @Override
    public void onReceived(String service, String action, State state, Packet packet) {
        switch (action) {
            case ContactAction.signIn:
                this.processSignIn(state, packet);
                break;

            case ContactAction.GetContact:
                this.processGetContact(state, packet);
                break;
        }
    }

    private void processSignIn(State state, Packet packet) {
        CubeCallback1<Contact> callback = CallBackManager.getCallBack(packet.sn);
        if (state.code == Service.StateOk) {
            Contact contact = new Contact(packet.data);
            callback.onSuccess(contact);
        } else {
            callback.onError(state.code, state.desc);
        }
    }

    private void processGetContact(State state, Packet packet) {
        CubeCallback1<Contact> callback = CallBackManager.getCallBack(packet.sn);
        if (state.code == Service.StateOk) {
            Contact contact = new Contact(packet.data);
            callback.onSuccess(contact);
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
        pipeline.send(Service.ContactService, packet);
    }
}
