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

import cube.contact.ContactService;
import cube.contact.ContactServiceEvent;
import cube.contact.model.ContactZoneBundle;
import cube.contact.model.Group;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.StableCompletionHandler;
import cube.core.handler.StableFailureHandler;
import cube.messaging.handler.DefaultConversationHandler;
import cube.messaging.model.Conversation;
import cube.messaging.model.ConversationState;
import cube.util.LogUtils;
import cube.util.ObservableEvent;
import cube.util.Observer;

/**
 * 消息模块用于监听其他模块事件的观察者。
 */
public class MessagingObserver implements Observer {

    private final static String TAG = "MessagingObserver";

    private MessagingService service;

    public MessagingObserver(MessagingService service) {
        this.service = service;
    }

    @Override
    public void update(ObservableEvent event) {
        Module module = (Module) event.getSubject();
        if (ContactService.NAME.equals(module.name)) {
            this.fireContactEvent(event);
        }
    }

    private void fireContactEvent(ObservableEvent event) {
        if (ContactServiceEvent.SelfReady.equals(event.name)) {
            synchronized (this) {
                if (!this.service.ready && !this.service.preparing.get()) {
                    // 准备数据
                    this.service.prepare(new StableCompletionHandler() {
                        @Override
                        public void handleCompletion(Module module) {
                            ObservableEvent event = new ObservableEvent(MessagingServiceEvent.Ready, service);
                            service.notifyObservers(event);
                        }
                    });
                }
            }
        }
        else if (ContactServiceEvent.GroupDismissed.equals(event.name)) {
            Group group = (Group) event.getData();
            Conversation conversation = null;
            synchronized (this.service) {
                for (Conversation conv : this.service.conversations) {
                    Group cg = conv.getGroup();
                    if (null == cg) {
                        continue;
                    }

                    if (cg.id.equals(group.id)) {
                        conversation = conv;
                        break;
                    }
                }
            }

            if (null != conversation) {
                if (conversation.getState() != ConversationState.Deleted) {
                    conversation.setState(ConversationState.Deleted);
                    this.service.updateConversation(conversation, new DefaultConversationHandler(false) {
                        @Override
                        public void handleConversation(Conversation conversation) {
                            // 从列表里删除
                            synchronized (service) {
                                service.conversations.remove(conversation);
                                service.conversationMessageListMap.remove(conversation.id);
                            }
                        }
                    }, new StableFailureHandler() {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            LogUtils.w(TAG, "GroupDismissed: " + error.code);
                        }
                    });
                }
            }
        }
        else if (ContactServiceEvent.ZoneParticipantRemoved.equals(event.name)) {
            ContactZoneBundle bundle = (ContactZoneBundle) event.getData();
            this.service.deleteConversation(bundle.participant.id, new DefaultConversationHandler(false) {
                @Override
                public void handleConversation(Conversation conversation) {
                    LogUtils.d(TAG, "ZoneParticipantRemoved");
                }
            }, new StableFailureHandler() {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    LogUtils.w(TAG, "ZoneParticipantRemoved: " + error.code);
                }
            });
        }
        else if (ContactServiceEvent.SignOut.equals(event.name)) {
            synchronized (this.service) {
                this.service.conversations.clear();
                this.service.conversationMessageListMap.clear();
            }
        }
    }
}
