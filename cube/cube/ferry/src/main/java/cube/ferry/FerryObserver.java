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

package cube.ferry;

import java.util.List;

import cube.auth.AuthService;
import cube.contact.ContactServiceEvent;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.StableFailureHandler;
import cube.ferry.handler.DefaultDetectHandler;
import cube.ferry.handler.DefaultTenetsHandler;
import cube.ferry.model.Tenet;
import cube.util.LogUtils;
import cube.util.ObservableEvent;
import cube.util.Observer;

/**
 * 摆渡模块观察者。
 */
public class FerryObserver implements Observer {

    private final static String TAG = "FerryObserver";

    private FerryService service;

    public FerryObserver(FerryService service) {
        this.service = service;
    }

    @Override
    public void update(ObservableEvent event) {
        String eventName = event.getName();
        if (ContactServiceEvent.SelfReady.equals(eventName)) {
            this.service.ready = true;

            this.service.execute(new Runnable() {
                @Override
                public void run() {
                    service.detectDomain(new DefaultDetectHandler(true) {
                        @Override
                        public void handleResult(boolean online, long duration) {
                            if (online) {
                                synchronized (service.listeners) {
                                    for (FerryEventListener listener : service.listeners) {
                                        listener.onFerryOnline(AuthService.getDomain());
                                    }
                                }
                            }
                            else {
                                synchronized (service.listeners) {
                                    for (FerryEventListener listener : service.listeners) {
                                        listener.onFerryOffline(AuthService.getDomain());
                                    }
                                }
                            }
                        }
                    });

                    service.takeOutTenets(new DefaultTenetsHandler(false) {
                        @Override
                        public void handleTenets(List<Tenet> tenetList) {
                            // 处理信条
                            for (Tenet tenet : tenetList) {
                                service.triggerTenet(tenet);
                            }
                        }
                    }, new StableFailureHandler() {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            LogUtils.w(TAG, "#update - #takeOutTenets failed: " + error.code);
                        }
                    });
                }
            });
        }
        else if (ContactServiceEvent.SelfLost.equals(eventName)) {
            // 更新就绪状态
            this.service.ready = false;

            // 删除域文件
            this.service.deleteDomainInfo();
        }
    }
}
