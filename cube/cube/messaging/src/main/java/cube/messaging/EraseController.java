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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.FailureHandler;
import cube.core.handler.StableFailureHandler;
import cube.messaging.extension.TypeableMessage;
import cube.messaging.handler.DefaultMessageHandler;
import cube.messaging.handler.EraseMessageHandler;
import cube.messaging.model.Message;

/**
 * 焚毁消息控制器。
 */
public class EraseController implements Runnable {

    private MessagingService service;

    private Message message;

    private int delayInSeconds;

    private EraseMessageHandler successHandler;

    private FailureHandler failureHandler;

    private AtomicBoolean running;

    private AtomicInteger countdown;

    private Timer timer;

    public EraseController(MessagingService service, Message message, int delayInSeconds,
                           EraseMessageHandler successHandler, FailureHandler failureHandler) {
        this.service = service;
        this.message = message;
        this.delayInSeconds = delayInSeconds;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.running = new AtomicBoolean(false);
        this.countdown = new AtomicInteger();
    }

    public void destroy() {
        if (null != this.timer) {
            this.timer.cancel();
            this.timer = null;
        }

        if (message instanceof TypeableMessage) {
            TypeableMessage typeableMessage = (TypeableMessage) message;
            typeableMessage.erase();
        }

        Object mutex = new Object();
        service.burnMessage(message, new DefaultMessageHandler(false) {
            @Override
            public void handleMessage(Message message) {
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
                mutex.wait(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        synchronized (service.eraseControllers) {
            service.eraseControllers.remove(this);
        }
    }

    @Override
    public void run() {
        this.running.set(true);
        this.countdown.set(this.delayInSeconds);

        if (this.successHandler.isInMainThread()) {
            this.service.executeOnMainThread(() -> {
                this.successHandler.onCountdownStarted(this.service,
                        this.message, this.countdown.get());
            });
        }
        else {
            this.service.execute(() -> {
                this.successHandler.onCountdownStarted(this.service,
                        this.message, this.countdown.get());
            });
        }

        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (countdown.get() <= 0) {
                    return;
                }

                int cd = countdown.decrementAndGet();

                if (successHandler.isInMainThread()) {
                    service.executeOnMainThread(() -> {
                        successHandler.onCountdownTick(service, message,
                                delayInSeconds - cd, delayInSeconds);
                    });
                }
                else {
                    service.execute(() -> {
                        successHandler.onCountdownTick(service, message,
                                delayInSeconds - cd, delayInSeconds);
                    });
                }

                if (cd == 0) {
                    processCompleted();
                }
            }
        }, 1000, 1000);
    }

    private void processCompleted() {
        // 擦除内容
        if (message instanceof TypeableMessage) {
            TypeableMessage typeableMessage = (TypeableMessage) message;
            typeableMessage.erase();
        }

        // 焚毁消息内容
        service.burnMessage(message, new DefaultMessageHandler() {
            @Override
            public void handleMessage(Message message) {
                synchronized (service.eraseControllers) {
                    service.eraseControllers.remove(EraseController.this);
                }
            }
        }, new StableFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                synchronized (service.eraseControllers) {
                    service.eraseControllers.remove(EraseController.this);
                }
            }
        });

        if (successHandler.isInMainThread()) {
            service.executeOnMainThread(() -> {
                successHandler.onCountdownCompleted(service, message);
            });
        }
        else {
            service.execute(() -> {
                successHandler.onCountdownCompleted(service, message);
            });
        }

        service.execute(() -> {
            timer.cancel();
            timer = null;
        });
    }
}
