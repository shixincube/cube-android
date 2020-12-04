package cube.message.impl;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import cube.common.callback.Callback;
import cube.common.callback.CubeCallback1;
import cube.common.callback.CubeCallback2;
import cube.common.callback.CubeCallback3;
import cube.common.callback.CubeCallbackUI1;
import cube.contact.service.ContactService;
import cube.contact.service.Self;
import cube.core.EngineAgent;
import cube.core.Module;
import cube.core.PluginSystem;
import cube.core.manager.CallBackManager;
import cube.message.service.MessageListener;
import cube.message.service.MessageService;
import cube.message.service.MessageState;
import cube.message.service.model.FileMessage;
import cube.message.service.model.Message;
import cube.pipeline.Packet;
import cube.pipeline.Pipeline;
import cube.pipeline.PipelineListener;
import cube.core.Service;
import cube.service.model.DeviceInfo;
import cube.message.service.model.PageInfo;
import cube.service.model.State;
import cube.utils.SpUtil;
import cube.utils.ThreadUtil;
import cube.utils.UIHandler;
import cube.utils.log.LogUtil;

/**
 * 消息服务实现
 *
 * @author LiuFeng
 * @data 2020/9/3 15:00
 */
public class MessageServiceImpl implements MessageService, Module, PipelineListener {
    private static final String TAG = "MessageServiceImpl";

    private Context mContext;
    private Pipeline pipeline;
    private ContactService contactService;

    private boolean isWorking;
    private List<MessageListener> listeners = new ArrayList<>();
    private Queue<Message> messageQueue = new ConcurrentLinkedQueue<>();
    private Map<Long, Message> sendMessageMap = new ConcurrentHashMap<>();
    private Map<Long, CubeCallback1<Message>> callbackMap = new ConcurrentHashMap<>();

    public MessageServiceImpl(Context context) {
        LogUtil.i(TAG, "MessageService:new");
        this.mContext = context;
    }

    @Override
    public String getName() {
        return MessageService.class.getSimpleName();
    }

    @Override
    public List<String> getRequires() {
        List<String> requires = new ArrayList<>();
        requires.add("ContactService");
        requires.add("FileStorage");
        return requires;
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
        pipeline.addListener(Service.MessageService, this);
        contactService = EngineAgent.getInstance().getService(ContactService.class);

        // 注册一个消息插件
        PluginSystem.getInstance().register(MessageHook.NAME, new MessagePlugin());

        // 添加消息钩子
        PluginSystem.getInstance().addHook(new MessageHook());
    }

    @Override
    public void stop() {
        pipeline.removeListener(Service.MessageService, this);
        PluginSystem.getInstance().deregister(MessageHook.NAME);
        PluginSystem.getInstance().removeHook(MessageHook.NAME);
    }

    /*public void sendToContact(Contact contact, Message message) {
        message.setFrom(SpUtil.getCubeId());
        message.setTo(String.valueOf(contact.getId()));
        message.setLocalTimestamp(System.currentTimeMillis());
        message.setState(MessageState.Sending);
        sendMessage(message);
    }

    public void sendToGroup(Group group, Message message) {
        message.setFrom(SpUtil.getCubeId());
        message.setTo(String.valueOf(group.getId()));
        message.setLocalTimestamp(System.currentTimeMillis());
        message.setState(MessageState.Sending);
        sendMessage(message);
    }*/

    @Override
    public void addListener(MessageListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(MessageListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    @Override
    public void sendMessage(Message message, CubeCallback1<Message> callback) {
        sendMessageMap.put(message.getId(), message);
        callbackMap.put(message.getId(), callback);
        sendMessage(message);
    }

    @Override
    public void receiptMessage(String sessionId, long timestamp, CubeCallback3<String, Long, DeviceInfo> callback) {

    }

    @Override
    public void recallMessage(long sn, CubeCallback1<Message> callback) {

    }

    @Override
    public void cancelFileMessage(long sn, CubeCallback1<Message> callback) {

    }

    @Override
    public void deleteMessage(long sn, CubeCallback1<Message> callback) {

    }

    @Override
    public boolean queryMessage(List<Long> sns, CubeCallback1<List<Message>> callback) {
        return false;
    }

    @Override
    public void queryMessage(String sessionId, long sinceTimestamp, long untilTimestamp, int offset, int count, CubeCallback2<PageInfo, List<Message>> callback) {

    }

    @Override
    public void syncMessage() {
        JSONObject param = new JSONObject();
        try {
            Self self = contactService.getSelf();
            param.put("id", SpUtil.getCubeId());
            param.put("device", self.getDevice().toJSON());
        } catch (JSONException e) {
            LogUtil.e(TAG, e);
        }

        send(MessageAction.Pull, param, null);
    }

    /**
     * 发送消息
     *
     * @param message
     */
    private void sendMessage(Message message) {
        if (message instanceof FileMessage) {
            processFileMessage((FileMessage) message);
        } else {
            message.setFrom(SpUtil.getCubeId());
            message.setState(MessageState.Sending);
            message.setSource(2020L);
            message.setLocalTimestamp(System.currentTimeMillis());

            dispatchMessage(message);
        }
    }

    private void processFileMessage(FileMessage message) {

    }


    /**
     * 分发消息
     *
     * @param message
     */
    public void dispatchMessage(Message message) {
        messageQueue.offer(message);

        if (!isWorking) {
            synchronized (this) {
                if (!isWorking) {
                    isWorking = true;
                    ThreadUtil.request(new Worker());
                }
            }
        }
    }

    /**
     * 消息发送任务
     */
    private class Worker implements Runnable {
        @Override
        public void run() {
            isWorking = true;

            while (!messageQueue.isEmpty()) {
                Message message = messageQueue.poll();
                if (message != null) {
                    // 触发消息钩子
                    message = PluginSystem.getInstance().getHook(MessageHook.NAME).apply(message);
                    CubeCallback1<Message> callback = callbackMap.remove(message.getId());
                    callback = callback != null ? new CubeCallbackUI1<>(callback) : null;
                    send(MessageAction.Push, message.toJSON(), callback);
                }
            }

            isWorking = false;
        }

    }

    @Override
    public void onReceived(String service, String action, State state, Packet packet) {
        switch (action) {
            case MessageAction.Push:
                this.processPush(state, packet);
                break;

            case MessageAction.Pull:
                this.processPull(state, packet);
                break;

            case MessageAction.Notify:
                this.processNotify(state, packet);
                break;
        }
    }

    private void processPush(State state, Packet packet) {
        LogUtil.i(TAG, "processPush --> data:" + packet.data);
        CubeCallback1<Message> callback = CallBackManager.getCallBack(packet.sn);
        if (state.code == Service.StateOk) {
            Message message = Message.create(packet.data);
            Message oldMessage = sendMessageMap.remove(message.getId());
            if (oldMessage == null) {
                oldMessage = message;
            }
            oldMessage.setState(MessageState.Succeed);
            oldMessage.setRemoteTimestamp(message.getRemoteTimestamp());

            if (callback != null) {
                callback.onSuccess(oldMessage);
            } else {
                for (MessageListener listener : listeners) {
                    listener.onMessageSent(oldMessage);
                }
            }
        } else {
            if (callback != null) {
                callback.onError(state.code, state.desc);
            } else {
                for (MessageListener listener : listeners) {
                    listener.onMessageError(state.code, state.desc);
                }
            }
        }
    }

    private void processPull(State state, Packet packet) {
        LogUtil.i(TAG, "processPull --> data:" + packet.data);
    }

    private void processNotify(State state, Packet packet) {
        LogUtil.i(TAG, "processNotify --> data:" + packet.data);
        Message message = Message.create(packet.data);
        UIHandler.run(() -> {
            for (MessageListener listener : listeners) {
                listener.onMessageReceived(message);
            }
        });
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
        pipeline.send(Service.MessageService, packet);
    }
}
