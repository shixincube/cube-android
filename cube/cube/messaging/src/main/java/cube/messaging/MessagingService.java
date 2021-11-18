/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.messaging;

import android.util.MutableBoolean;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import cube.contact.ContactService;
import cube.contact.ContactServiceEvent;
import cube.contact.model.Contact;
import cube.contact.model.Group;
import cube.contact.model.Self;
import cube.core.Hook;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.MutableModuleError;
import cube.core.Packet;
import cube.core.PipelineState;
import cube.core.handler.CompletionHandler;
import cube.core.handler.DefaultFailureHandler;
import cube.core.handler.FailureHandler;
import cube.core.handler.PipelineHandler;
import cube.core.handler.StableCompletionHandler;
import cube.core.handler.StableFailureHandler;
import cube.fileprocessor.FileProcessor;
import cube.fileprocessor.model.FileThumbnail;
import cube.filestorage.FileStorage;
import cube.filestorage.handler.StableDownloadFileHandler;
import cube.filestorage.handler.StableUploadFileHandler;
import cube.filestorage.model.FileAnchor;
import cube.filestorage.model.FileLabel;
import cube.messaging.extension.MessageTypePlugin;
import cube.messaging.handler.ConversationHandler;
import cube.messaging.handler.DefaultConversationHandler;
import cube.messaging.handler.DefaultSendHandler;
import cube.messaging.handler.LoadAttachmentHandler;
import cube.messaging.handler.MessageListResultHandler;
import cube.messaging.handler.SendHandler;
import cube.messaging.hook.InstantiateHook;
import cube.messaging.model.CacheableFileLabelCapsule;
import cube.messaging.model.Conversation;
import cube.messaging.model.ConversationReminded;
import cube.messaging.model.ConversationState;
import cube.messaging.model.ConversationType;
import cube.messaging.model.FileAttachment;
import cube.messaging.model.Message;
import cube.messaging.model.MessageScope;
import cube.messaging.model.MessageState;
import cube.messaging.model.MessageType;
import cube.messaging.model.NullMessage;
import cube.util.LogUtils;
import cube.util.ObservableEvent;

/**
 * 消息传输服务。
 */
public class MessagingService extends Module {

    public final static String NAME = "Messaging";

    private final static String TAG = MessagingService.class.getSimpleName();

    private long retrospectDuration = 30L * 24L * 60L * 60000L;

    private long blockTimeout = 3L * 60L * 1000L;

    private MessagingPipelineListener pipelineListener;

    private MessagingStorage storage;

    private MessagingObserver observer;

    private ContactService contactService;

    private FileStorage fileStorage;

    private FileProcessor fileProcessor;

    private MessagingRecentEventListener recentEventListener;

    private List<MessageEventListener> eventListeners;

    private Map<Long, List<MessageEventListener>> conversationMessageListeners;

    private AtomicBoolean preparing;

    private boolean ready;

    private long lastMessageTime;

    private Timer pullTimer;
    private CompletionHandler pullCompletionHandler;

    private List<Conversation> conversations;

    /**
     * 预载的最近会话数量。
     */
    private int preloadConversationRecentNum;

    /**
     * 预载最近会话的消息数量。
     */
    private int preloadConversationMessageNum;

    /**
     * 会话对应的消息列表。
     */
    private Map<Long, MessageList> conversationMessageListMap;

    /**
     * 更新对应附录里的文件数据时记录的信息。
     */
    private Map<String, CacheableFileLabelCapsule> capsuleCache;

    /**
     * 正在发送的消息清单。
     */
    private Queue<Message> sendingList;

    public MessagingService() {
        super(MessagingService.NAME);
        this.pipelineListener = new MessagingPipelineListener(this);
        this.storage = new MessagingStorage(this);
        this.observer = new MessagingObserver(this);
        this.preparing = new AtomicBoolean(false);
        this.ready = false;
        this.lastMessageTime = 0;
        this.conversationMessageListMap = new ConcurrentHashMap<>();
        this.sendingList = new ConcurrentLinkedQueue<>();
        this.preloadConversationRecentNum = 5;
        this.preloadConversationMessageNum = 10;
        this.capsuleCache = new ConcurrentHashMap<>();
    }

    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }

        // 启用文件存储器
        if (this.kernel.hasModule(FileStorage.NAME)) {
            this.fileStorage = (FileStorage) this.kernel.getModule(FileStorage.NAME);
            this.fileStorage.start();
        }

        // 启动文件处理器
        if (this.kernel.hasModule(FileProcessor.NAME)) {
            this.fileProcessor = (FileProcessor) this.kernel.getModule(FileProcessor.NAME);
            this.fileProcessor.start();
        }

        // 组装插件
        this.assemble();

        this.pipeline.addListener(MessagingService.NAME, this.pipelineListener);

        // 监听联系人模块
        this.contactService = (ContactService) this.kernel.getModule(ContactService.NAME);
        this.contactService.attachWithName(ContactServiceEvent.SelfReady, this.observer);
        this.contactService.attachWithName(ContactServiceEvent.SignIn, this.observer);
        this.contactService.attachWithName(ContactServiceEvent.SignOut, this.observer);

        synchronized (this) {
            if (null != this.contactService.getSelf() && !this.ready && !this.preparing.get()) {
                this.execute(() -> {
                    prepare(new StableCompletionHandler() {
                        @Override
                        public void handleCompletion(Module module) {
                            ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Ready, MessagingService.this);
                            notifyObservers(event);
                        }
                    });
                });
            }
        }

        ThumbnailDownloadManager.getInstance().setExecutor(this.kernel.getExecutor());

        this.kernel.getInspector().depositMap(this.capsuleCache);

        return true;
    }

    @Override
    public void stop() {
        super.stop();

        // 拆除插件
        this.dissolve();

        this.kernel.getInspector().withdrawMap(this.capsuleCache);

        this.contactService.detachWithName(ContactServiceEvent.SelfReady, this.observer);
        this.contactService.detachWithName(ContactServiceEvent.SignIn, this.observer);
        this.contactService.detachWithName(ContactServiceEvent.SignOut, this.observer);

        this.pipeline.removeListener(MessagingService.NAME, this.pipelineListener);

        // 关闭存储
        this.storage.close();
    }

    @Override
    public boolean isReady() {
        return this.ready;
    }

    @Override
    protected void config(@Nullable JSONObject configData) {
        // Nothing
    }

    /**
     * 设置消息服务最近事件监听器。
     *
     * @param listener 监听器。
     */
    public void setRecentEventListener(MessagingRecentEventListener listener) {
        this.recentEventListener = listener;
    }

    /**
     * 添加消息事件监听器。
     *
     * @param listener 消息监听器。
     */
    public void addEventListener(MessageEventListener listener) {
        if (null == this.eventListeners) {
            this.eventListeners = new Vector<>();
        }

        if (!this.eventListeners.contains(listener)) {
            this.eventListeners.add(listener);
        }
    }

    /**
     * 移除消息事件监听器。
     *
     * @param listener 消息监听器。
     */
    public void removeEventListener(MessageEventListener listener) {
        if (null == this.eventListeners) {
            return;
        }

        this.eventListeners.remove(listener);
    }

    /**
     * 添加指定会话的消息事件监听器。
     *
     * @param conversation 指定会话。
     * @param listener 消息事件监听器。
     */
    public void addEventListener(Conversation conversation, MessageEventListener listener) {
        if (null == this.conversationMessageListeners) {
            this.conversationMessageListeners = new HashMap<>();
        }

        List<MessageEventListener> list = this.conversationMessageListeners.get(conversation.id);
        if (null == list) {
            list = new Vector<>();
            list.add(listener);
            this.conversationMessageListeners.put(conversation.id, list);
        }
        else {
            list.add(listener);
        }
    }

    /**
     * 移除指定会话的消息事件监听器。
     *
     * @param conversation 指定会话。
     * @param listener 消息事件监听器。
     */
    public void removeEventListener(Conversation conversation, MessageEventListener listener) {
        if (null == this.conversationMessageListeners) {
            return;
        }

        List<MessageEventListener> list = this.conversationMessageListeners.get(conversation.id);
        if (null != list) {
            list.remove(listener);
        }
    }

    private void assemble() {
        this.pluginSystem.addHook(new InstantiateHook());

        // 注册插件
        this.pluginSystem.registerPlugin(InstantiateHook.NAME, new MessageTypePlugin());
    }

    private void dissolve() {
        this.pluginSystem.clearHooks();
        this.pluginSystem.clearPlugins();
    }

    /**
     * 设置预载消息的数量。
     *
     * @param recentConversationNum
     * @param messageNum
     */
    public void setPreloadConversationMessageNum(int recentConversationNum, int messageNum) {
        this.preloadConversationRecentNum = recentConversationNum;
        this.preloadConversationMessageNum = messageNum;
    }

    /**
     * 获取指定 ID 的消息实例。
     *
     * @param messageId
     * @return
     */
    public Message getMessageById(Long messageId) {
        Message message = this.storage.readMessageNoFillById(messageId);
        return this.fillMessage(message);
    }

    /**
     * 获取最近的会话清单。
     *
     * @return 返回消息列表。如果返回 {@code null} 值表示消息服务模块未启动。
     */
    public List<Conversation> getRecentConversations() {
        return this.getRecentConversations(20);
    }

    /**
     * 获取最近的会话清单。
     *
     * @param max 指定最大记录数量。
     * @return 返回消息列表。如果返回 {@code null} 值表示消息服务模块未启动。
     */
    public List<Conversation> getRecentConversations(int max) {
        if (!this.hasStarted()) {
            return null;
        }

        if (!this.ready) {
            synchronized (this.preparing) {
                try {
                    this.preparing.wait(5000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        synchronized (this) {
            if (null == this.conversations || this.conversations.isEmpty()) {
                List<Conversation> list = this.storage.queryRecentConversations(max);

                if (null == this.conversations) {
                    this.conversations = new Vector<>(list);
                }
                else {
                    this.conversations.clear();
                    this.conversations.addAll(list);
                }

                this.sortConversationList(this.conversations);

                // 进行预载
                if (this.preloadConversationRecentNum > 0 && this.preloadConversationMessageNum > 0) {
                    LogUtils.i(TAG,
                            "Preload conversation messages: " + preloadConversationRecentNum + " - " + preloadConversationMessageNum);
                    this.execute(() -> {
                        long now = System.currentTimeMillis();
                        for (int i = 0, len = conversations.size(); i < len && i < preloadConversationRecentNum; ++i) {
                            Conversation conv = conversations.get(i);
                            getRecentMessages(conv, preloadConversationMessageNum);
                        }
                        LogUtils.i(TAG,
                                "Preload conversation messages elapsed: " + (System.currentTimeMillis() - now) + " ms");
                    });
                }
            }
        }

        return this.conversations;
    }

    /**
     * 获取指定 ID 的会话。
     *
     * @param id 指定会话 ID 。
     * @return 返回会话实例。
     */
    public Conversation getConversation(Long id) {
        Conversation conversation = null;

        synchronized (this) {
            if (null == this.conversations || this.conversations.isEmpty()) {
                conversation = this.storage.readConversation(id);
            }
            else {
                for (Conversation conv : this.conversations) {
                    if (conv.id.longValue() == id.longValue()) {
                        conversation = conv;
                        break;
                    }
                }
            }
        }

        if (null == conversation) {
            // 从存储里读取
            conversation = this.storage.readConversation(id);

            if (null == conversation) {
                // 创建新的会话
                Contact contact = this.contactService.getContact(id);
                if (null != contact) {
                    conversation = new Conversation(this.contactService.getSelf(), contact, ConversationReminded.Normal);
                }
                else {
                    // TODO Group
                }
            }
        }

        return conversation;
    }

    /**
     * 标记指定会话里的所有消息为已读。
     *
     * @param conversation 指定会话。
     */
    public void markRead(final Conversation conversation) {
        final Object mutex = new Object();

        this.markRead(conversation, new DefaultConversationHandler(false) {
            @Override
            public void handleConversation(Conversation conversation) {
                synchronized (mutex) {
                    mutex.notify();
                }
            }
        }, new StableFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                synchronized (mutex) {
                    mutex.notify();
                }
            }
        });

        synchronized (mutex) {
            try {
                mutex.wait(this.blockTimeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 标记指定会话里的所有消息为已读。
     *
     * @param conversation 指定会话。
     * @param conversationHandler 指定回调句柄。
     * @param failureHandler 指定故障回调句柄。
     */
    public void markRead(final Conversation conversation, ConversationHandler conversationHandler, FailureHandler failureHandler) {
        if (conversation.getUnreadCount() == 0) {
            if (conversationHandler.isInMainThread()) {
                executeOnMainThread(() -> {
                    conversationHandler.handleConversation(conversation);
                });
            }
            else {
                execute(() -> {
                    conversationHandler.handleConversation(conversation);
                });
            }
            return;
        }

        MessageList messageList = this.conversationMessageListMap.get(conversation.id);
        if (null != messageList) {
            for (Message message : messageList.messages) {
                message.setState(MessageState.Read);
            }
        }

        if (null != this.conversations) {
            Conversation current = null;
            for (Conversation conv : this.conversations) {
                if (conv.id.longValue() == conversation.id.longValue()) {
                    current = conv;
                    break;
                }
            }

            if (null != current) {
                current.getRecentMessage().setState(MessageState.Read);
                current.setUnreadCount(0);
            }
        }

        conversation.getRecentMessage().setState(MessageState.Read);
        conversation.setUnreadCount(0);

        this.execute(() -> {
            // 更新会话
            storage.updateConversation(conversation);

            // 更新会话相关的数据
            List<Long> idList = storage.updateMessageState(conversation, MessageState.Sent, MessageState.Read);

            if (idList.isEmpty()) {
                // 没有记录
                if (conversationHandler.isInMainThread()) {
                    executeOnMainThread(() -> {
                        conversationHandler.handleConversation(conversation);
                    });
                }
                else {
                    conversationHandler.handleConversation(conversation);
                }
                return;
            }

            if (!pipeline.isReady()) {
                ModuleError error = new ModuleError(MessagingService.NAME, MessagingServiceState.PipelineFault.code);
                error.data = conversation;
                if (failureHandler.isInMainThread()) {
                    executeOnMainThread(() -> {
                        failureHandler.handleFailure(MessagingService.this, error);
                    });
                }
                else {
                    failureHandler.handleFailure(MessagingService.this, error);
                }
                return;
            }

            JSONArray array = new JSONArray();
            for (Long id : idList) {
                array.put(id.longValue());
            }

            // 发送请求到服务器
            JSONObject payload = new JSONObject();
            try {
                payload.put("contactId", contactService.getSelf().id.longValue());
                payload.put("messageIdList", array);
                if (conversation.getType() == ConversationType.Contact) {
                    payload.put("messageFrom", conversation.getPivotalId().longValue());
                }
                else if (conversation.getType() == ConversationType.Group) {
                    payload.put("messageSource", conversation.getPivotalId().longValue());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // 与服务器同步
            Packet requestPacket = new Packet(MessagingAction.Read, payload);
            pipeline.send(MessagingService.NAME, requestPacket, new PipelineHandler() {
                @Override
                public void handleResponse(Packet packet) {
                    if (packet.state.code != PipelineState.Ok.code) {
                        LogUtils.w(TAG, "#markRead - code : " + packet.state.code);

                        ModuleError error = new ModuleError(MessagingService.NAME, packet.state.code);
                        error.data = conversation;
                        if (failureHandler.isInMainThread()) {
                            executeOnMainThread(() -> {
                                failureHandler.handleFailure(MessagingService.this, error);
                            });
                        }
                        else {
                            execute(() -> {
                                failureHandler.handleFailure(MessagingService.this, error);
                            });
                        }
                        return;
                    }

                    int stateCode = packet.extractServiceStateCode();
                    if (stateCode != MessagingServiceState.Ok.code) {
                        LogUtils.w(TAG, "#markRead - code : " + stateCode);

                        ModuleError error = new ModuleError(MessagingService.NAME, stateCode);
                        error.data = conversation;
                        if (failureHandler.isInMainThread()) {
                            executeOnMainThread(() -> {
                                failureHandler.handleFailure(MessagingService.this, error);
                            });
                        }
                        else {
                            execute(() -> {
                                failureHandler.handleFailure(MessagingService.this, error);
                            });
                        }
                        return;
                    }

                    // 更新消息在服务器上的状态
                    storage.updateMessagesRemoteState(idList, MessageState.Read);

                    if (conversationHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            conversationHandler.handleConversation(conversation);
                        });
                    }
                    else {
                        execute(() -> {
                            conversationHandler.handleConversation(conversation);
                        });
                    }
                }
            });
        });
    }

    /**
     * 将指定会话的一般状态修改为关注状态。
     *
     * @param conversation 指定会话。
     * @return 设置成功返回 {@code true} ，否则返回 {@code false} 。
     */
    public boolean focusOnConversation(Conversation conversation) {
        final MutableBoolean mutex = new MutableBoolean(false);

        this.focusOnConversation(conversation, new DefaultConversationHandler(false) {
            @Override
            public void handleConversation(Conversation conversation) {
                mutex.value = true;
                synchronized (mutex) {
                    mutex.notify();
                }
            }
        }, new StableFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                synchronized (mutex) {
                    mutex.notify();
                }
            }
        });

        synchronized (mutex) {
            try {
                mutex.wait(this.blockTimeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return mutex.value;
    }

    /**
     * 将指定会话的一般状态修改为关注状态。
     *
     * @param conversation 指定会话。
     * @param conversationHandler 指定回调句柄。
     * @param failureHandler 指定故障回调句柄。
     */
    public void focusOnConversation(Conversation conversation, ConversationHandler conversationHandler, FailureHandler failureHandler) {
        ConversationState state = conversation.getState();
        if (ConversationState.Important == state) {
            if (conversationHandler.isInMainThread()) {
                this.executeOnMainThread(() -> {
                    conversationHandler.handleConversation(conversation);
                });
            }
            else {
                this.execute(() -> {
                    conversationHandler.handleConversation(conversation);
                });
            }
            return;
        }

        // 修改状态
        conversation.setState(ConversationState.Important);
        // 排序
        if (!this.conversations.contains(conversation)) {
            this.conversations.add(conversation);
        }
        this.sortConversationList(this.conversations);

        if (!this.updateConversation(conversation, conversationHandler, failureHandler)) {
            // 更新流程没有执行，将状态修改为原状态
            conversation.setState(ConversationState.Normal);
        }
    }

    /**
     * 将指定会话的关注状态修改为一般状态。
     *
     * @param conversation 指定会话。
     * @return 设置成功返回 {@code true} ，否则返回 {@code false} 。
     */
    public boolean focusOutConversation(Conversation conversation) {
        final MutableBoolean mutex = new MutableBoolean(false);

        this.focusOutConversation(conversation, new DefaultConversationHandler(false) {
            @Override
            public void handleConversation(Conversation conversation) {
                mutex.value = true;
                synchronized (mutex) {
                    mutex.notify();
                }
            }
        }, new StableFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                synchronized (mutex) {
                    mutex.notify();
                }
            }
        });

        synchronized (mutex) {
            try {
                mutex.wait(this.blockTimeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return mutex.value;
    }

    /**
     * 将指定会话的关注状态修改为一般状态。
     *
     * @param conversation 指定会话。
     * @param conversationHandler 指定回调句柄。
     * @param failureHandler 指定故障回调句柄。
     */
    public void focusOutConversation(Conversation conversation, ConversationHandler conversationHandler, FailureHandler failureHandler) {
        ConversationState state = conversation.getState();
        if (ConversationState.Normal == state) {
            if (conversationHandler.isInMainThread()) {
                this.executeOnMainThread(() -> {
                    conversationHandler.handleConversation(conversation);
                });
            }
            else {
                this.execute(() -> {
                    conversationHandler.handleConversation(conversation);
                });
            }
            return;
        }

        // 修改状态
        conversation.setState(ConversationState.Normal);
        // 排序
        if (!this.conversations.contains(conversation)) {
            this.conversations.add(conversation);
        }
        this.sortConversationList(this.conversations);

        if (!this.updateConversation(conversation, conversationHandler, failureHandler)) {
            // 更新流程没有执行，将状态修改为原状态
            conversation.setState(ConversationState.Important);
        }
    }

    /**
     * 执行会话更新流程。
     *
     * @param conversation
     * @param conversationHandler
     * @param failureHandler
     * @return
     */
    private boolean updateConversation(Conversation conversation, ConversationHandler conversationHandler, FailureHandler failureHandler) {
        if (!this.pipeline.isReady()) {
            ModuleError error = new ModuleError(MessagingService.NAME, MessagingServiceState.PipelineFault.code);
            error.data = conversation;
            if (failureHandler.isInMainThread()) {
                this.executeOnMainThread(() -> {
                    failureHandler.handleFailure(MessagingService.this, error);
                });
            }
            else {
                this.execute(() -> {
                    failureHandler.handleFailure(MessagingService.this, error);
                });
            }

            return false;
        }

        Packet requestPacket = new Packet(MessagingAction.UpdateConversation, conversation.toJSON());
        this.pipeline.send(MessagingService.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(MessagingService.NAME, packet.state.code);
                    error.data = conversation;
                    if (failureHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            failureHandler.handleFailure(MessagingService.this, error);
                        });
                    }
                    else {
                        execute(() -> {
                            failureHandler.handleFailure(MessagingService.this, error);
                        });
                    }
                    return;
                }

                int state = packet.extractServiceStateCode();
                if (state != MessagingServiceState.Ok.code) {
                    ModuleError error = new ModuleError(MessagingService.NAME, state);
                    error.data = conversation;
                    if (failureHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            failureHandler.handleFailure(MessagingService.this, error);
                        });
                    }
                    else {
                        execute(() -> {
                            failureHandler.handleFailure(MessagingService.this, error);
                        });
                    }
                    return;
                }

                try {
                    Conversation responseConversation = new Conversation(packet.extractServiceData());
                    conversation.setState(responseConversation.getState());
                    conversation.setReminded(responseConversation.getReminded());
                } catch (JSONException e) {
                    LogUtils.w(TAG, "#updateConverstion", e);
                }

                // 更新数据库
                storage.updateConversation(conversation);

                if (conversationHandler.isInMainThread()) {
                    executeOnMainThread(() -> {
                        conversationHandler.handleConversation(conversation);
                    });
                }
                else {
                    execute(() -> {
                        conversationHandler.handleConversation(conversation);
                    });
                }
            }
        });

        return true;
    }

    /**
     * 获取会话的最近消息。
     *
     * @param conversation
     * @param limit
     * @return
     */
    public MessageListResult getRecentMessages(Conversation conversation, int limit) {
        MessageList list = this.conversationMessageListMap.get(conversation.id);
        if (null != list) {
            if (!list.messages.isEmpty()) {
                // 延长实体寿命
                list.extendLife(3L * 60L * 1000L);

                final List<Message> resultList = new ArrayList<>(list.messages);
                final boolean hasMore = list.hasMore;

                return new MessageListResult() {
                    @Override
                    public List<Message> getList() {
                        return resultList;
                    }

                    @Override
                    public boolean hasMore() {
                        return hasMore;
                    }
                };
            }
        }
        else {
            list = new MessageList();
            this.conversationMessageListMap.put(conversation.id, list);
            // 托管列表内的实体生命周期
            this.kernel.getInspector().depositList(list.messages);
        }

        if (conversation.getType() == ConversationType.Contact) {
            MessageListResult result = this.storage.queryMessagesByReverseWithContact(conversation.getContact().getId(),
                    System.currentTimeMillis(), limit);
            // 从数据库里查出来的是时间倒序，从大到小
            // 这里对列表进行翻转，翻转为时间正序
            Collections.reverse(result.getList());

            // 重置列表
            list.reset(result);

            return result;
        }
        else if (conversation.getType() == ConversationType.Group) {
            // TODO
        }

        return null;
    }

    /**
     * 查询会话在指定消息之前的历史消息强清单。
     *
     * @param conversation
     * @param message
     * @param limit
     * @param handler
     */
    public void queryMessages(Conversation conversation, Message message, int limit, MessageListResultHandler handler) {
        this.execute(new Runnable() {
            @Override
            public void run() {
                if (ConversationType.Contact == conversation.getType()) {
                    // 如果消息为空指针，则查询最近的消息
                    if (null == message) {
                        MessageListResult result = getRecentMessages(conversation, limit);
                        executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                handler.handle(result.getList(), result.hasMore());
                            }
                        });
                        return;
                    }

                    // 从数据库查询
                    MessageListResult result = storage.queryMessagesByReverseWithContact(conversation.getPivotalId(), message.getRemoteTimestamp(), limit);

                    MessageList list = conversationMessageListMap.get(conversation.id);
                    if (null != list) {
                        list.insertMessages(result.getList());
                    }

                    executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            handler.handle(result.getList(), result.hasMore());
                        }
                    });
                }
                else if (ConversationType.Group == conversation.getType()) {
                    // TODO
                }
            }
        });
    }

    public void sendTypingStatus(Conversation conversation) {
        // TODO
    }

    /**
     * 向指定会话的参与人发送消息。
     *
     * @param conversation
     * @param message
     * @param <T> 消息实例类型。
     */
    public <T extends Message> void sendMessage(final Conversation conversation, final T message) {
        final Object mutex = new Object();

        this.sendMessage(conversation, message, new DefaultSendHandler<Conversation, T>(false) {
            @Override
            public void handleProcessing(Conversation destination, T processingMessage) {
                // Nothing
            }
            @Override
            public void handleProcessed(Conversation destination, T processedMessage) {
                // Nothing
            }
            @Override
            public void handleSending(Conversation destination, T sendingMessage) {
                // Nothing
            }
            @Override
            public void handleSent(Conversation destination, T sentMessage) {
                synchronized (mutex) {
                    mutex.notify();
                }
            }
        }, new DefaultFailureHandler(false) {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                synchronized (mutex) {
                    mutex.notify();
                }
            }
        });

        synchronized (mutex) {
            try {
                mutex.wait(30L * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 向指定联系人发送消息。
     *
     * @param contact
     * @param message
     * @param <T> 消息实例类型。
     */
    public <T extends Message> void sendMessage(final Contact contact, final T message) {
        final Object mutex = new Object();

        this.sendMessage(contact, message, new DefaultSendHandler<Contact, T>(false) {
            @Override
            public void handleProcessing(Contact destination, T processingMessage) {
                // Nothing
            }
            @Override
            public void handleProcessed(Contact destination, T processedMessage) {
                // Nothing
            }
            @Override
            public void handleSending(Contact destination, T sendingMessage) {
                // Nothing
            }
            @Override
            public void handleSent(Contact destination, T sentMessage) {
                synchronized (mutex) {
                    mutex.notify();
                }
            }
        }, new DefaultFailureHandler(false) {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                synchronized (mutex) {
                    mutex.notify();
                }
            }
        });

        synchronized (mutex) {
            try {
                mutex.wait(30L *1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 向指定群组发送消息。
     *
     * @param group
     * @param message
     * @param <T> 消息实例类型。
     */
    public <T extends Message> void sendMessage(final Group group, final T message) {
        final Object mutex = new Object();

        this.sendMessage(group, message, new DefaultSendHandler<Group, T>(false) {
            @Override
            public void handleProcessing(Group destination, T processingMessage) {
                // Nothing
            }
            @Override
            public void handleProcessed(Group destination, T processedMessage) {
                // Nothing
            }
            @Override
            public void handleSending(Group destination, T sendingMessage) {
                // Nothing
            }
            @Override
            public void handleSent(Group destination, T sentMessage) {
                synchronized (mutex) {
                    mutex.notify();
                }
            }
        }, new DefaultFailureHandler(false) {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                synchronized (mutex) {
                    mutex.notify();
                }
            }
        });

        synchronized (mutex) {
            try {
                mutex.wait(30L * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 向指定会话发送消息。
     *
     * @param conversation
     * @param message
     * @param sendHandler
     * @param <T>
     */
    public <T extends Message> void sendMessage(final Conversation conversation, final T message,
                                                SendHandler<Conversation, T> sendHandler) {
        this.sendMessage(conversation, message, sendHandler, new StableFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                // Nothing
            }
        });
    }

    /**
     * 向指定会话发送消息。
     *
     * @param conversation
     * @param message
     * @param sendHandler
     * @param failureHandler
     * @param <T> 消息实例类型。
     */
    public <T extends Message> void sendMessage(final Conversation conversation, final T message,
                            SendHandler<Conversation, T> sendHandler, FailureHandler failureHandler) {
        if (ConversationType.Contact == conversation.getType()) {
            this.sendMessage(conversation.getContact(), message, new SendHandler<Contact, T>() {
                @Override
                public void handleProcessing(Contact destination, T message) {
                    sendHandler.handleProcessing(conversation, message);
                }

                @Override
                public void handleProcessed(Contact destination, T message) {
                    sendHandler.handleProcessed(conversation, message);
                }

                @Override
                public void handleSending(Contact destination, T message) {
                    sendHandler.handleSending(conversation, message);
                }

                @Override
                public void handleSent(Contact destination, T message) {
                    // 完成后，对会话进行数据处理
                    appendMessageToConversation(conversation.id, message);

                    sendHandler.handleSent(conversation, message);
                }

                @Override
                public boolean isInMainThread() {
                    return sendHandler.isInMainThread();
                }
            }, failureHandler);

            this.tryAddConversation(conversation);
        }
        else if (ConversationType.Group == conversation.getType()) {
            this.sendMessage(conversation.getGroup(), message, new SendHandler<Group, T>() {
                @Override
                public void handleProcessing(Group destination, T message) {
                    sendHandler.handleProcessing(conversation, message);
                }

                @Override
                public void handleProcessed(Group destination, T message) {
                    sendHandler.handleProcessed(conversation, message);
                }

                @Override
                public void handleSending(Group destination, T message) {
                    sendHandler.handleSending(conversation, message);
                }

                @Override
                public void handleSent(Group destination, T message) {
                    // 完成后，对会话进行数据处理
                    appendMessageToConversation(conversation.id, message);

                    sendHandler.handleSent(conversation, message);
                }

                @Override
                public boolean isInMainThread() {
                    return sendHandler.isInMainThread();
                }
            }, failureHandler);

            this.tryAddConversation(conversation);
        }
    }

    /**
     * 向指定联系人发送消息。
     *
     * @param contact
     * @param message
     * @param sendHandler
     * @param failureHandler
     * @param <T> 消息实例类型。
     */
    public <T extends Message> void sendMessage(Contact contact, T message, SendHandler<Contact, T> sendHandler, FailureHandler failureHandler) {
        // 消息数据赋值
        message.assign(this.contactService.getSelf().id, contact.id, 0);
        // 设置状态为正在发送
        message.setState(MessageState.Sending);
        // 填写数据
        this.fillMessage(message);

        // 异步执行发送流程
        this.execute(() -> {
            processSend(message, new DefaultSendHandler<Object, T>() {
                @Override
                public void handleProcessing(Object destination, T message) {
                    if (sendHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            sendHandler.handleProcessing(contact, message);
                        });
                    }
                    else {
                        sendHandler.handleProcessing(contact, message);
                    }
                }

                @Override
                public void handleProcessed(Object destination, T message) {
                    if (sendHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            sendHandler.handleProcessed(contact, message);
                        });
                    }
                    else {
                        sendHandler.handleProcessed(contact, message);
                    }
                }

                @Override
                public void handleSending(Object destination, T message) {
                    if (sendHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            sendHandler.handleSending(contact, message);
                        });
                    }
                    else {
                        sendHandler.handleSending(contact, message);
                    }
                }

                @Override
                public void handleSent(Object destination, T message) {
                    if (sendHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            sendHandler.handleSent(contact, message);
                        });
                    }
                    else {
                        sendHandler.handleSent(contact, message);
                    }
                }
            }, new StableFailureHandler() {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    if (failureHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            failureHandler.handleFailure(module, error);
                        });
                    }
                    else {
                        failureHandler.handleFailure(module, error);
                    }
                }
            });
        });
    }

    /**
     * 向指定群组发送消息。
     *
     * @param group
     * @param message
     * @param sendHandler
     * @param failureHandler
     */
    public <T extends Message> void sendMessage(Group group, Message message, SendHandler<Group, T> sendHandler, FailureHandler failureHandler) {
        // TODO
    }

    /**
     * 加载消息附件到本地。
     *
     * @param message
     * @param loadHandler
     * @param failureHandler
     * @param <T>
     */
    public <T extends Message> void loadMessageAttachment(T message, LoadAttachmentHandler<T> loadHandler,
                                                          FailureHandler failureHandler) {
        FileAttachment fileAttachment = message.getAttachment();
        if (null == fileAttachment) {
            ModuleError error = new ModuleError(NAME, MessagingServiceState.AttachmentError.code);
            if (failureHandler.isInMainThread()) {
                executeOnMainThread(() -> {
                    failureHandler.handleFailure(MessagingService.this, error);
                });
            }
            else {
                execute(() -> {
                    failureHandler.handleFailure(MessagingService.this, error);
                });
            }
            return;
        }

        if (message.getState() == MessageState.Sending) {
            if (loadHandler.isInMainThread()) {
                executeOnMainThread(() -> {
                    loadHandler.handleLoaded(message, fileAttachment);
                });
            }
            else {
                execute(() -> {
                    loadHandler.handleLoaded(message, fileAttachment);
                });
            }
            return;
        }

        if (fileAttachment.existsPrefLocal()) {
            if (loadHandler.isInMainThread()) {
                executeOnMainThread(() -> {
                    loadHandler.handleLoaded(message, fileAttachment);
                });
            }
            else {
                execute(() -> {
                    loadHandler.handleLoaded(message, fileAttachment);
                });
            }
            return;
        }

        final String fileCode = fileAttachment.getPrefFileCode();
        boolean downloading = false;
        synchronized (this.capsuleCache) {
            CacheableFileLabelCapsule capsule = this.capsuleCache.get(fileCode);
            if (null == capsule) {
                capsule = new CacheableFileLabelCapsule();
                this.capsuleCache.put(fileCode, capsule);
            }
            else {
                capsule.entityLifeExpiry += LIFESPAN;
                downloading = true;
            }

            capsule.addMessage(message, fileAttachment.getPrefFileLabel(), loadHandler, failureHandler);
        }

        if (!downloading) {
            // 下载数据
            this.fileStorage.downloadFile(fileAttachment.getPrefFileLabel(), new StableDownloadFileHandler() {
                @Override
                public void handleStarted(FileAnchor anchor) {
                }

                @Override
                public void handleProcessing(FileAnchor anchor) {
                    synchronized (capsuleCache) {
                        CacheableFileLabelCapsule capsule = capsuleCache.get(anchor.getFileCode());
                        if (null != capsule) {
                            for (CacheableFileLabelCapsule.Capsule current : capsule.capsuleList) {
                                final LoadAttachmentHandler loadHandler = current.loadHandler;
                                if (loadHandler.isInMainThread()) {
                                    final Message curMessage = current.message;
                                    executeOnMainThread(() -> {
                                        loadHandler.handleLoading((T) curMessage, curMessage.getAttachment(), anchor);
                                    });
                                }
                                else {
                                    loadHandler.handleLoading((T) current.message, current.message.getAttachment(), anchor);
                                }
                            }
                        }
                    }
                }

                @Override
                public void handleSuccess(FileAnchor anchor, FileLabel fileLabel) {
                    synchronized (capsuleCache) {
                        CacheableFileLabelCapsule capsule = capsuleCache.get(anchor.getFileCode());
                        if (null != capsule) {
                            HashMap<Long, Message> messageMap = new HashMap<>();

                            for (CacheableFileLabelCapsule.Capsule current : capsule.capsuleList) {
                                // 设置消息附件
                                current.fileLabel.setFilePath(anchor.getFilePath());
                                current.message.getAttachment().matchPrefFile(anchor.getFile());

                                // 放入 Map
                                if (!messageMap.containsKey(current.message.id)) {
                                    messageMap.put(current.message.id, current.message);
                                }

                                final LoadAttachmentHandler loadHandler = current.loadHandler;
                                if (loadHandler.isInMainThread()) {
                                    final Message curMessage = current.message;
                                    executeOnMainThread(() -> {
                                        loadHandler.handleLoaded((T) curMessage, curMessage.getAttachment());
                                    });
                                }
                                else {
                                    loadHandler.handleLoaded((T) current.message, current.message.getAttachment());
                                }
                            }

                            for (Message message : messageMap.values()) {
                                storage.updateMessageAttachment(message.id, message.getAttachment());
                            }
                            messageMap.clear();

                            // 清空
                            capsuleCache.remove(anchor.getFileCode());
                        }
                    }
                }

                @Override
                public void handleFailure(ModuleError error, @Nullable FileAnchor anchor) {
                    synchronized (capsuleCache) {
                        CacheableFileLabelCapsule capsule = capsuleCache.get(anchor.getFileCode());
                        if (null != capsule) {
                            for (CacheableFileLabelCapsule.Capsule current : capsule.capsuleList) {
                                final FailureHandler handler = current.failureHandler;
                                if (handler.isInMainThread()) {
                                    final ModuleError currentError = new ModuleError(NAME, error.code);
                                    currentError.data = current.message;
                                    executeOnMainThread(() -> {
                                        handler.handleFailure(MessagingService.this, currentError);
                                    });
                                }
                                else {
                                    ModuleError currentError = new ModuleError(NAME, error.code);
                                    currentError.data = current.message;
                                    handler.handleFailure(MessagingService.this, currentError);
                                }
                            }

                            capsuleCache.remove(anchor.getFileCode());
                        }
                    }
                }
            });
        }
    }

    private void tryAddConversation(Conversation conversation) {
        if (!this.conversations.contains(conversation)) {
            this.conversations.add(conversation);
            this.sortConversationList(this.conversations);

            this.storage.writeConversation(conversation);
        }
    }

    /**
     * 进行发送处理。
     *
     * @param message
     * @param sendHandler 消息操作句柄。
     * @param failureHandler
     */
    private void processSend(Message message,
                             SendHandler sendHandler,
                             FailureHandler failureHandler) {
        // 加入正在发送队列
        this.sendingList.offer(message);

        this.execute(() -> {
            sendHandler.handleProcessing(null, message);

            // 产生事件
            ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Processing, message);
            notifyObservers(event);
        });

        // 处理文件附件
        FileAttachment fileAttachment = message.getAttachment();
        if (null != fileAttachment && null != this.fileProcessor) {
            if (fileAttachment.isCompressed()) {
                // 需要压缩文件
                if (fileAttachment.isPrefImageType()) {
                    // 图片生成缩略图
                    FileThumbnail fileThumbnail = this.fileProcessor.makeImageThumbnail(fileAttachment.getPrefFile());
                    // 使用缩略图作为文件
                    fileAttachment.reset(fileThumbnail.getFile());

                    LogUtils.d(TAG, "Thumbnail : " + fileThumbnail.print());
                }
                else {
                    // TODO
                }
            }
        }

        this.execute(() -> {
            sendHandler.handleProcessed(null, message);

            // 产生事件
            ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Processed, message);
            notifyObservers(event);
        });

        if (!this.pipeline.isReady() && message.getScope() == MessageScope.Unlimited) {
            // 修改状态
            message.setState(MessageState.Fault);

            // 更新数据库
            storage.updateMessage(message);

            // 不从正在发送列表删除该消息

            this.execute(() -> {
                ModuleError error = new ModuleError(MessagingService.NAME, MessagingServiceState.PipelineFault.code);
                error.data = message;
                failureHandler.handleFailure(MessagingService.this, error);

                // 产生事件
                ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Fault, error);
                notifyObservers(event);
            });

            return;
        }

        // 处理文件附件
        if (null != fileAttachment) {
            MutableModuleError moduleError = new MutableModuleError();
            // 计数
            AtomicInteger count = new AtomicInteger(fileAttachment.numAnchors());

            // 上传附录里的文件
            this.uploadAttachment(fileAttachment, new StableUploadFileHandler() {
                @Override
                public void handleStarted(FileAnchor anchor) {
                    // Nothing
                }

                @Override
                public void handleProcessing(FileAnchor anchor) {
                    // 回调
                    sendHandler.handleSending(null, message);

                    // 产生事件
                    ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Sending, message);
                    notifyObservers(event);
                }

                @Override
                public void handleSuccess(FileAnchor anchor, FileLabel fileLabel) {
                    // 文件上传成功，将 Label 与 Anchor 进行匹配
                    fileAttachment.matchFileLabel(anchor, fileLabel);

                    if (0 == count.decrementAndGet()) {
                        synchronized (fileAttachment) {
                            fileAttachment.notify();
                        }
                    }
                }

                @Override
                public void handleFailure(ModuleError error, @Nullable FileAnchor anchor) {
                    moduleError.value = error;

                    synchronized (fileAttachment) {
                        fileAttachment.notify();
                    }
                }
            });

            synchronized (fileAttachment) {
                try {
                    fileAttachment.wait(10L * 60L * 1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (null != moduleError.value) {
                // 发生错误
                moduleError.value.data = message;
                failureHandler.handleFailure(MessagingService.this, moduleError.value);

                // 产生事件
                ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Fault, moduleError.value);
                notifyObservers(event);
                return;
            }
        }
        else {
            this.execute(() -> {
                sendHandler.handleSending(null, message);

                // 产生事件
                ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Sending, message);
                notifyObservers(event);
            });
        }

        if (message.getScope() == MessageScope.Private) {
            // 仅在本地生效的消息
            // 移除正在发送数据
            removeSendingMessage(message);

            // 修改状态
            message.setState(MessageState.Read);
            // 修改远程时间戳
            message.setRemoteTS(System.currentTimeMillis());

            // 更新时间
            if (message.getRemoteTimestamp() > lastMessageTime) {
                lastMessageTime = message.getRemoteTimestamp();
            }

            // 更新数据库
            storage.updateMessage(message);

            this.execute(() -> {
                // 回调
                sendHandler.handleSent(null, message);

                // 产生事件
                ObservableEvent event = new ObservableEvent(MessagingServiceEvent.MarkOnlyOwner, message);
                notifyObservers(event);
            });

            return;
        }

        Packet packet = new Packet(MessagingAction.Push, message.toCompactJSON());
        this.pipeline.send(MessagingService.NAME, packet, new PipelineHandler() {
            @Override
            public void handleResponse(Packet responsePacket) {
                if (responsePacket.state.code != PipelineState.Ok.code) {
                    // 修改状态
                    message.setState(MessageState.Fault);

                    // 更新数据库
                    storage.updateMessage(message);

                    ModuleError error = new ModuleError(MessagingService.NAME, MessagingServiceState.ServerFault.code);
                    error.data = message;
                    failureHandler.handleFailure(MessagingService.this, error);

                    // 产生事件
                    ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Fault, error);
                    notifyObservers(event);
                    return;
                }

                int stateCode = responsePacket.extractServiceStateCode();
                JSONObject data = responsePacket.extractServiceData();

                // 移除正在发送数据
                removeSendingMessage(message);

                try {
                    // 提取应答的数据
                    long responseRTS = data.getLong("rts");
                    MessageState responseState = MessageState.parse(data.getInt("state"));

                    // 更新时间戳
                    message.setRemoteTS(responseRTS);
                    // 更新状态
                    message.setState(responseState);

                    // 更新时间戳
                    if (message.getRemoteTimestamp() > lastMessageTime) {
                        lastMessageTime = message.getRemoteTimestamp();
                    }

                    // 服务器会为图片类型文件自动生成缩略图，缩略图以文件标签上下文数据形式进行存储。
                    if (null != fileAttachment && data.has("attachment")) {
                        FileAttachment responseAttachment = new FileAttachment(data.getJSONObject("attachment"));
                        message.getAttachment().update(responseAttachment);
                    }

                    // 更新数据库
                    storage.updateMessage(message);

                    if (stateCode == MessagingServiceState.Ok.code) {
                        if (message.getScope() == MessageScope.Unlimited) {
                            // 回调
                            sendHandler.handleSent(null, message);

                            // 产生事件
                            ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Sent, message);
                            notifyObservers(event);
                        }
                        else {
                            // 回调
                            sendHandler.handleSent(null, message);

                            // 产生事件
                            ObservableEvent event = new ObservableEvent(MessagingServiceEvent.MarkOnlyOwner, message);
                            notifyObservers(event);
                        }
                    }
                    else if (stateCode == MessagingServiceState.BeBlocked.code) {
                        ObservableEvent event = null;

                        if (message.getState() == MessageState.SendBlocked) {
                            // 回调
                            sendHandler.handleSent(null, message);

                            event = new ObservableEvent(MessagingServiceEvent.SendBlocked, message);
                        }
                        else if (message.getState() == MessageState.ReceiveBlocked) {
                            // 回调
                            sendHandler.handleSent(null, message);

                            event = new ObservableEvent(MessagingServiceEvent.ReceiveBlocked, message);
                        }
                        else {
                            ModuleError error = new ModuleError(MessagingService.NAME, stateCode);
                            error.data = message;
                            // 回调
                            failureHandler.handleFailure(MessagingService.this, error);

                            event = new ObservableEvent(MessagingServiceEvent.Fault, error);
                        }

                        // 产生事件
                        notifyObservers(event);
                    }
                    else {
                        ModuleError error = new ModuleError(MessagingService.NAME, stateCode);
                        error.data = message;

                        // 回调
                        failureHandler.handleFailure(MessagingService.this, error);

                        // 产生事件
                        ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Fault, error);
                        notifyObservers(event);
                    }
                } catch (Exception e) {
                    LogUtils.w(TAG, e);

                    ModuleError error = new ModuleError(MessagingService.NAME, MessagingServiceState.DataStructureError.code);
                    error.data = message;
                    // 回调
                    failureHandler.handleFailure(MessagingService.this, error);

                    // 产生事件
                    ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Fault, error);
                    notifyObservers(event);
                }
            }
        });
    }

    private void removeSendingMessage(Message message) {
        Iterator<Message> iterator = this.sendingList.iterator();
        while (iterator.hasNext()) {
            Message current = iterator.next();
            if (current.id.longValue() == message.id.longValue()) {
                iterator.remove();
                break;
            }
        }
    }

    private void uploadAttachment(FileAttachment attachment, StableUploadFileHandler handler) {
        FileStorage fileStorage = (FileStorage) this.kernel.getModule(FileStorage.NAME);
        for (int i = 0, length = attachment.numAnchors(); i < length; ++i) {
            FileAnchor anchor = attachment.getFileAnchor(i);
            fileStorage.uploadFile(anchor, handler);
        }
    }

    private void prepare(StableCompletionHandler handler) {
        this.preparing.set(true);

        Self self = this.contactService.getSelf();

        // 开启存储
        this.storage.open(this.getContext(), self.id, self.domain);

        long now = System.currentTimeMillis();

        MutableBoolean first = new MutableBoolean(false);

        // 查询本地最近消息时间
        long time = this.storage.queryLastMessageTime();
        if (0 == time) {
            first.value = true;
            this.lastMessageTime = now - this.retrospectDuration;
        }
        else {
            this.lastMessageTime = time;
        }

        // 服务就绪
        this.ready = true;

        // 进行线程通知
        synchronized (this.preparing) {
            this.preparing.notifyAll();
        }

        if (!first.value) {
            // 不是第一次获取数据或者未能连接到服务器，直接回调
            this.execute(() -> {
                handler.handleCompletion(MessagingService.this);
            });
        }

        MutableBoolean gotMessages = new MutableBoolean(false);
        MutableBoolean gotConversations = new MutableBoolean(false);

        // 从服务器上拉取自上一次时间戳之后的所有消息
        this.queryRemoteMessage(this.lastMessageTime + 1, now, new StableCompletionHandler() {
            @Override
            public void handleCompletion(Module module) {
                gotMessages.value = true;
                if (gotConversations.value) {
                    preparing.set(false);
                    if (first.value) {
                        handler.handleCompletion(MessagingService.this);
                    }
                }
            }
        });

        // 获取最新的会话列表
        // 根据最近一次消息时间戳更新数量
        int limit = 30;
        if (now - this.lastMessageTime > 1 * 60 * 60 * 1000) {
            // 大于一小时
            limit = 50;
        }
        this.queryRemoteConversations(limit, new StableCompletionHandler() {
            @Override
            public void handleCompletion(Module module) {
                gotConversations.value = true;
                if (gotMessages.value) {
                    preparing.set(false);
                    if (first.value) {
                        handler.handleCompletion(MessagingService.this);
                    }
                }
            }
        });
    }

    /**
     * 从服务器上获取指定时间范围内的消息。
     *
     * @param beginning
     * @param ending
     * @param completionHandler
     */
    private void queryRemoteMessage(long beginning, long ending, StableCompletionHandler completionHandler) {
        LogUtils.d(TAG, "#queryRemoteMessage : " + Math.floor((ending - beginning) / 1000.0 / 60.0) + " min");

        if (!this.pipeline.isReady() && this.isAvailableNetwork()) {
            synchronized (this) {
                try {
                    this.wait(5000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // 如果没有网络直接回调函数
        if (!this.pipeline.isReady()) {
            completionHandler.handleCompletion(this);
            LogUtils.d(TAG, "#queryRemoteMessage - Pipeline is not ready");
            return;
        }

        if (null != this.pullTimer) {
            LogUtils.d(TAG, "#queryRemoteMessage - Timer is null");
            return;
        }

        // 检测是否已经签入
        int count = 1000;
        while (!this.contactService.isReady()) {
            if (--count <= 0) {
                break;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.pullTimer = new Timer();
        this.pullTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                completionHandler.handleCompletion(MessagingService.this);

                try {
                    pullTimer.cancel();
                } catch (Exception e) {
                    // Nothing
                }

                pullTimer = null;
            }
        }, 10L * 1000L);

        this.pullCompletionHandler = completionHandler;

        Self self = this.contactService.getSelf();

        JSONObject payload = new JSONObject();
        try {
            payload.put("id", self.id.longValue());
            payload.put("domain", self.domain);
            payload.put("device", self.device.toJSON());
            payload.put("beginning", beginning);
            payload.put("ending", ending);
        } catch (JSONException e) {
            // Nothing
        }

        // 向服务器请求数据
        Packet packet = new Packet(MessagingAction.Pull, payload);
        this.pipeline.send(MessagingService.NAME, packet);
    }

    /**
     * 查询服务器上的会话列表。
     *
     * @param limit
     * @param completionHandler
     */
    private void queryRemoteConversations(int limit, StableCompletionHandler completionHandler) {
        if (!this.pipeline.isReady() && this.isAvailableNetwork()) {
            synchronized (this) {
                try {
                    this.wait(5000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!this.pipeline.isReady()) {
            completionHandler.handleCompletion(null);
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("limit", limit);
        } catch (JSONException e) {
            // Nothing
        }
        Packet requestPacket = new Packet(MessagingAction.GetConversations, payload);
        this.pipeline.send(MessagingService.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    completionHandler.handleCompletion(null);
                    return;
                }

                if (packet.extractServiceStateCode() != MessagingServiceState.Ok.code) {
                    completionHandler.handleCompletion(null);
                    return;
                }

                List<Conversation> conversationList = new ArrayList<>();
                JSONObject data = packet.extractServiceData();
                try {
                    // 读取列表
                    JSONArray list = data.getJSONArray("list");
                    for (int i = 0; i < list.length(); ++i) {
                        Conversation conversation = new Conversation(list.getJSONObject(i));
                        fillConversation(conversation);
                        conversationList.add(conversation);
                    }
                } catch (JSONException e) {
                    LogUtils.d(TAG,
                            "#queryRemoteConversations : error conversation list format");
                }

                execute(() -> {
                    if (conversationList.isEmpty()) {
                        // 回调
                        completionHandler.handleCompletion(MessagingService.this);
                        return;
                    }

                    boolean changed = true;
                    Conversation lastConversation = storage.getLastConversation();
                    Conversation firstConversation = conversationList.get(0);
                    if (null != lastConversation && lastConversation.equals(firstConversation)) {
                        // 本地存储的最近会话与服务器返回的一致，数据没有变化
                        // 这样判断会有误判，但是仅需要比较一条记录
                        changed = false;
                    }

                    // 更新到数据库
                    storage.updateConversations(conversationList);

                    // 回调
                    completionHandler.handleCompletion(MessagingService.this);

                    // 回调事件监听器
                    if (changed && null != recentEventListener) {
                        synchronized (MessagingService.this) {
                            if (null != conversations) {
                                conversations.clear();
                            }
                        }

                        recentEventListener.onConversationListUpdated(getRecentConversations(), MessagingService.this);
                    }
                });
            }
        });
    }

    @Override
    public void notifyObservers(ObservableEvent event) {
        super.notifyObservers(event);

        String eventName = event.getName();
        if (MessagingServiceEvent.Notify.equals(eventName)) {
            Message message = (Message) event.getData();

            if (null != this.eventListeners) {
                for (MessageEventListener listener : this.eventListeners) {
                    listener.onMessageReceived(message, this);
                }
            }

            if (null != this.conversationMessageListeners) {
                Long convId = message.isFromGroup() ? message.getSource() : message.getPartnerId();
                List<MessageEventListener> list = this.conversationMessageListeners.get(convId);
                if (null != list) {
                    for (MessageEventListener listener : list) {
                        listener.onMessageReceived(message, this);
                    }
                }
            }
        }
        else if (MessagingServiceEvent.Processing.equals(eventName)) {
            Message message = (Message) event.getData();
            if (null != this.eventListeners) {
                for (MessageEventListener listener : this.eventListeners) {
                    listener.onMessageProcessing(message, this);
                }
            }
        }
    }

    private void appendMessageToConversation(Long conversationId, Message message) {
        MessageList list = this.conversationMessageListMap.get(conversationId);
        if (null != list) {
            list.appendMessage(message);
        }

        Conversation conversation = null;
        if (null != this.conversations && !this.conversations.isEmpty()) {
            for (Conversation current : this.conversations) {
                if (current.getPivotalId().longValue() == conversationId) {
                    conversation = current;
                    break;
                }
            }
        }

        // 跳过私域消息
        if (message.getScope() == MessageScope.Private) {
            if (null != conversation) {
                conversation.setTimestamp(message.getLocalTimestamp());
                this.sortConversationList(this.conversations);
            }
            return;
        }

        // 更新消息
        conversation.setRecentMessage(message);
        this.sortConversationList(this.conversations);

        // 更新会话数据库
        this.storage.updateRecentMessage(conversationId, message);
    }

    protected void fireContactEvent(ObservableEvent event) {
        if (ContactServiceEvent.SelfReady.equals(event.name)) {
            synchronized (this) {
                if (!this.ready && !this.preparing.get()) {
                    // 准备数据
                    this.prepare(new StableCompletionHandler() {
                        @Override
                        public void handleCompletion(Module module) {
                            ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Ready, MessagingService.this);
                            notifyObservers(event);
                        }
                    });
                }
            }
        }
        else if (ContactServiceEvent.SignOut.equals(event.name)) {
            // TODO
        }
    }

    protected void triggerNotify(JSONObject data) {
        Message message = null;
        try {
            message = new Message(this, data);
        } catch (JSONException e) {
            LogUtils.w(TAG, "#triggerNotify", e);
        }

        // 填充消息相关实体对象
        Message compMessage = this.fillMessage(message);

        // 数据写入数据库
        boolean exists = this.storage.updateMessage(message);

        if (message.getRemoteTimestamp() > this.lastMessageTime) {
            this.lastMessageTime = message.getRemoteTimestamp();
        }

        if (!exists) {
            // 更新会话清单
            long pivotalId = compMessage.isFromGroup() ? compMessage.getSource() : compMessage.getPartnerId();
            this.appendMessageToConversation(pivotalId, compMessage);

            ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Notify, compMessage);
            this.notifyObservers(event);
        }
    }

    protected void triggerPull(int code, JSONObject data) {
        if (null != this.pullTimer) {
            this.pullTimer.cancel();

            // 触发回调，通知应用已收到服务器数据
            if (null != this.pullCompletionHandler) {
                this.execute(() -> {
                    pullCompletionHandler.handleCompletion(MessagingService.this);
                    pullCompletionHandler = null;
                });
            }

            this.pullTimer = null;
        }

        if (code != MessagingServiceState.Ok.code) {
            LogUtils.w(TAG, "#triggerPull state : " + code);
            return;
        }

        try {
            int total = data.getInt("total");
            long beginning = data.getLong("beginning");
            long ending = data.getLong("ending");
            JSONArray messages = data.getJSONArray("messages");

            LogUtils.d(TAG, "Pull messages total: " + total);

            for (int i = 0; i < messages.length(); ++i) {
                JSONObject json = messages.getJSONObject(i);
                this.triggerNotify(json);
            }

            // 对消息进行状态对比
            // 如果服务器状态与本地状态不一致，将服务器上的状态修改为本地状态
        } catch (JSONException e) {
            LogUtils.w(TAG, e);
        }
    }

    /**
     * 填充消息数据各属性的实例。
     *
     * @param message
     * @return
     */
    protected Message fillMessage(Message message) {
        Self self = this.contactService.getSelf();
        message.setSelfTyper(message.getFrom() == self.id.longValue());

        // 设置服务
        message.setService(this);

        // 发件人
        message.setSender(this.contactService.getContact(message.getFrom()));

        if (message.isFromGroup()) {
            // TODO 获取群组
        }
        else {
            // 收件人
            message.setReceiver(this.contactService.getContact(message.getTo()));
        }

        // 预载入附件
        FileAttachment attachment = message.getAttachment();
        if (message.getState() != MessageState.Sending && null != attachment) {
            FileLabel fileLabel = null;
            if (attachment.isCompressed()) {
                // 压缩模式下直接下载附件原文件
                if (!attachment.existsPrefLocal()) {
                    fileLabel = attachment.getPrefFileLabel();
                }
            }

            if (null != fileLabel) {
                final String fileCode = fileLabel.getFileCode();
                boolean downloading = false;
                synchronized (this.capsuleCache) {
                    // 填写缩略图的 File Code
                    CacheableFileLabelCapsule capsule = this.capsuleCache.get(fileCode);
                    if (null == capsule) {
                        capsule = new CacheableFileLabelCapsule();
                        this.capsuleCache.put(fileCode, capsule);
                    }
                    else {
                        // 更新寿命
                        capsule.entityLifeExpiry += LIFESPAN;
                        downloading = true;
                    }

                    // 添加附件对应的消息
                    capsule.addMessage(message, fileLabel);
                }

                LogUtils.d(TAG, "Download file : " + fileLabel.getFileCode() +
                        " (" +  message.id + ") - " + message.getFrom() + " -> " + message.getTo());

                if (!downloading) {
                    // 没有正在下载，对文件进行下载
                    this.fileStorage.downloadFile(fileLabel, new StableDownloadFileHandler() {
                        @Override
                        public void handleStarted(FileAnchor anchor) {
                            // Nothing
                        }

                        @Override
                        public void handleProcessing(FileAnchor anchor) {
                            // Nothing
                        }

                        @Override
                        public void handleSuccess(FileAnchor anchor, FileLabel fileLabel) {
                            LogUtils.d(TAG, "#fillMessage - download success : " +
                                    "(" + anchor.getFileCode() + ") " + anchor.getFileName() +
                                    " -> " + fileLabel.getFilePath());

                            synchronized (capsuleCache) {
                                CacheableFileLabelCapsule capsule = capsuleCache.get(anchor.getFileCode());
                                if (null != capsule) {
                                    HashMap<Long, Message> messageMap = new HashMap<>();

                                    CacheableFileLabelCapsule.Capsule current = capsule.capsuleList.poll();
                                    while (null != current) {
                                        // 设置文件路径
                                        current.fileLabel.setFilePath(anchor.getFilePath());
                                        current.message.getAttachment().matchPrefFile(anchor.getFile());

                                        if (!messageMap.containsKey(message.id)) {
                                            messageMap.put(message.id, message);
                                        }

                                        // next
                                        current = capsule.capsuleList.poll();
                                    }

                                    for (Message message : messageMap.values()) {
                                        storage.updateMessageAttachment(message.id, message.getAttachment());
                                    }
                                }
                            }
                        }

                        @Override
                        public void handleFailure(ModuleError error, @Nullable FileAnchor anchor) {
                            // Nothing
                        }
                    });
                }
            }

            // 如果附件里的文件标签关联了缩略图，本地没有，下载缩略图到本地
            // 缩略图的管理的流程和预处理附件需要分开
            FileLabel faLabel = attachment.getPrefFileLabel();
            JSONObject context = faLabel.getContext();
            if (null != context) {
                // 尝试将上下文转为缩略图对象
                FileThumbnail thumbnail = null;
                try {
                    thumbnail = new FileThumbnail(context);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (null != thumbnail) {
                    if (!thumbnail.existsLocal()) {
                        FileLabel thumbLabel = thumbnail.getFileLabel();
                        if (null != thumbLabel) {
                            ThumbnailDownloadTask task = new ThumbnailDownloadTask(fileStorage,
                                    storage, message, faLabel, thumbnail);
                            ThumbnailDownloadManager.getInstance().schedule(task);
                        }
                    }
                }
            }
        }

        // 实例化
        if (message.getType() == MessageType.Unknown) {
            Hook<Message> hook = (Hook<Message>) this.pluginSystem.getHook(InstantiateHook.NAME);
            Message compMessage = hook.apply(message);
            return compMessage;
        }
        else {
            return message;
        }
    }

    protected void fillConversation(Conversation conversation) {
        if (conversation.getRecentMessage().getFrom() == 0) {
            conversation.setRecentMessage(new NullMessage(this.contactService.getSelf(),
                    this.contactService.getContact(conversation.getPivotalId())));
        }

        // 填充消息
        Message recentMessage = this.fillMessage(conversation.getRecentMessage());
        conversation.setRecentMessage(recentMessage);

        if (ConversationType.Contact == conversation.getType()) {
            Contact contact = this.contactService.getContact(conversation.getPivotalId());
            conversation.setPivotal(contact);
        }
        else if (ConversationType.Group == conversation.getType()) {
            // TODO
        }
    }

    private void sortConversationList(List<Conversation> list) {
        List<Conversation> normalList = new ArrayList<>();

        // 将 Important 置顶
        Collections.sort(list, new Comparator<Conversation>() {
            @Override
            public int compare(Conversation c1, Conversation c2) {
                if (c1.getTimestamp() < c2.getTimestamp()) {
                    return 1;
                }
                else if (c1.getTimestamp() > c2.getTimestamp()) {
                    return -1;
                }
                else {
                    return 0;
                }
            }
        });

        Iterator<Conversation> iterator = list.iterator();
        while (iterator.hasNext()) {
            Conversation conversation = iterator.next();
            if (conversation.getState() == ConversationState.Normal ||
                conversation.getState() == ConversationState.Deleted) {
                // 添加到临时表
                normalList.add(conversation);
                iterator.remove();
            }
        }

        list.addAll(normalList);
    }
}
