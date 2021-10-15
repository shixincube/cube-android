/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Shixin Cube Team.
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

package cube.auth;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import cube.auth.handler.AuthTokenHandler;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.Packet;
import cube.core.handler.PipelineHandler;

/**
 * 授权服务模块。
 */
public class AuthService extends Module {

    public final static String NAME = "Auth";

    protected final static String ACTION_APPLY_TOKEN = "applyToken";

    private AuthToken token;

    private String domain;

    private Timer timer;

    public AuthService() {
        super(AuthService.NAME);
    }

    public AuthToken getToken() {
        return this.token;
    }

    @Override
    public void stop() {
        super.stop();

        synchronized (this) {
            if (null != this.timer) {
                this.timer.cancel();
                this.timer = null;
            }
        }
    }

    @Override
    public boolean isReady() {
        return true;
    }

    public void check(final String domain, final String appKey, final AuthTokenHandler handler) {
        this.execute(new Runnable() {
            @Override
            public void run() {
                final AuthStorage storage = new AuthStorage();
                if (!storage.open(getContext())) {
                    handler.handleFailure(new ModuleError(NAME, AuthServiceState.StorageError.code));
                    return;
                }

                AuthToken token = storage.loadToken(domain, appKey);
                if (null != token && token.isValid()) {
                    storage.close();

                    AuthService.this.token = token;

                    handler.handleSuccess(token);
                }
                else {
                    waitPipelineReady(new Runnable() {
                        @Override
                        public void run() {
                            if (!pipeline.isReady()) {
                                storage.close();
                                // 超时
                                ModuleError error = new ModuleError(NAME, AuthServiceState.Timeout.code, "Pipeline timeout");
                                handler.handleFailure(error);
                                return;
                            }

                            // 申请令牌

                        }
                    });
                }
            }
        });
    }

    protected void applyToken(String domain, String appKey, AuthTokenHandler handler) {
        JSONObject data = new JSONObject();
        try {
            data.put("domain", domain);
            data.put("appKey", appKey);
        } catch (JSONException e) {
            handler.handleFailure(new ModuleError(NAME, AuthServiceState.DataFormatError.code));
            return;
        }

        Packet packet = new Packet(ACTION_APPLY_TOKEN, data);
        this.pipeline.send(AuthService.NAME, packet, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {

            }
        });
    }

    private void waitPipelineReady(final Runnable task) {
        synchronized (this) {
            if (null == this.timer) {
                this.timer = new Timer();
                this.timer.schedule(new PipelineWaitingTask(task), 100, 100);
            }
        }
    }

    private class PipelineWaitingTask extends TimerTask {

        private Runnable task;

        private int count = 50;

        public PipelineWaitingTask(Runnable task) {
            super();
            this.task = task;
        }

        private void fire() {
            AuthService.this.execute(new Runnable() {
                @Override
                public void run() {
                    task.run();
                }
            });

            (new Thread() {
                @Override
                public void run() {
                    synchronized (AuthService.this) {
                        AuthService.this.timer.cancel();
                        AuthService.this.timer = null;
                    }
                }
            }).start();
        }

        @Override
        public void run() {
            --this.count;

            if (pipeline.isReady()) {
                fire();
            }
            else if (this.count <= 0) {
                // 超时
                fire();
            }
        }
    }
}
