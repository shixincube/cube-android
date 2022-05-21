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

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cube.auth.model.AuthDomain;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.Packet;
import cube.core.PipelineState;
import cube.core.handler.FailureHandler;
import cube.core.handler.PipelineHandler;
import cube.ferry.handler.DomainHandler;
import cube.ferry.model.DomainMember;
import cube.util.LogUtils;

/**
 * 数据摆渡服务。
 */
public class FerryService extends Module {

    private final static String TAG = FerryService.class.getSimpleName();

    /**
     * 模块名。
     */
    public final static String NAME = "Ferry";

    private FerryPipelineListener pipelineListener;

    public FerryService() {
        super(NAME);
    }

    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }

        this.pipelineListener = new FerryPipelineListener(this);
        this.pipeline.addListener(NAME, this.pipelineListener);

        return true;
    }

    @Override
    public void stop() {
        super.stop();

        if (null != this.pipelineListener) {
            this.pipeline.removeListener(NAME, this.pipelineListener);
            this.pipelineListener = null;
        }
    }

    @Override
    protected void config(@Nullable JSONObject configData) {
        // Nothing
    }

    @Override
    public boolean isReady() {
        return true;
    }

    /**
     * 获取指定名称的访问域。
     *
     * @param domainName
     * @param successHandler
     * @param failureHandler
     */
    public void getAuthDomain(String domainName, DomainHandler successHandler,
                              FailureHandler failureHandler) {
        if (!this.pipeline.isReady()) {
            // 数据通道未就绪
            ModuleError error = new ModuleError(NAME, FerryServiceState.NoNetwork.code);
            execute(failureHandler, error);
            return;
        }

        JSONObject packetData = new JSONObject();
        try {
            packetData.put("domain", domainName);
        } catch (JSONException e) {
            LogUtils.w(TAG, "#getAuthDomain", e);
        }

        Packet requestPacket = new Packet(FerryServiceAction.QueryDomain, packetData);
        this.pipeline.send(FerryService.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    ModuleError error = new ModuleError(FerryService.NAME, packet.state.code);
                    execute(failureHandler, error);
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != FerryServiceState.Ok.code) {
                    ModuleError error = new ModuleError(FerryService.NAME, stateCode);
                    execute(failureHandler, error);
                    return;
                }

                JSONObject data = packet.extractServiceData();
                try {
                    JSONObject domainJson = data.getJSONObject("domain");
                    AuthDomain authDomain = new AuthDomain(domainJson);

                    List<DomainMember> members = new ArrayList<>();
                    JSONArray array = data.getJSONArray("members");
                    // TODO read list

                    if (successHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            successHandler.handleDomain(authDomain, members);
                        });
                    }
                    else {
                        successHandler.handleDomain(authDomain, members);
                    }
                } catch (JSONException e) {
                    LogUtils.w(TAG, e);
                    ModuleError error = new ModuleError(FerryService.NAME,
                            FerryServiceState.DataFormatError.code);
                    execute(failureHandler, error);
                }
            }
        });
    }
}
