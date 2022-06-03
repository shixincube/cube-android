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

import android.app.Activity;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import cube.auth.AuthService;
import cube.auth.model.AuthDomain;
import cube.contact.ContactService;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.Packet;
import cube.core.PipelineState;
import cube.core.handler.FailureHandler;
import cube.core.handler.PipelineHandler;
import cube.ferry.handler.DetectHandler;
import cube.ferry.handler.DomainHandler;
import cube.ferry.handler.DomainMemberHandler;
import cube.ferry.handler.TenetsHandler;
import cube.ferry.model.DomainInfo;
import cube.ferry.model.DomainMember;
import cube.ferry.model.JoinWay;
import cube.util.FileUtils;
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

    private List<FerryEventListener> listeners;

    private AtomicBoolean houseOnline;

    private boolean membership;

    public FerryService() {
        super(NAME);
        this.listeners = new ArrayList<>();
        this.houseOnline = new AtomicBoolean(false);
        this.membership = false;
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

        this.houseOnline.set(false);
        this.membership = false;
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
     * 添加事件监听器。
     *
     * @param listener 监听器实例。
     */
    public void addEventListener(FerryEventListener listener) {
        synchronized (this.listeners) {
            if (this.listeners.contains(listener)) {
                return;
            }

            this.listeners.add(listener);
        }
    }

    /**
     * 移除事件监听器。
     *
     * @param listener 监听器实例。
     */
    public void removeEventListener(FerryEventListener listener) {
        synchronized (this.listeners) {
            this.listeners.remove(listener);
        }
    }

    /**
     * House 主机是否在线。
     *
     * @return 如果 House 主机在线返回 {@code true} 。
     */
    public boolean isHouseOnline() {
        return this.houseOnline.get();
    }

    /**
     * 探测域。
     *
     * @param handler 探测结果回调句柄。
     */
    public void detectDomain(DetectHandler handler) {
        if (!this.pipeline.isReady()) {
            if (handler.isInMainThread()) {
                executeOnMainThread(() -> {
                    handler.handleResult(false, 0);
                });
            }
            else {
                execute(() -> {
                    handler.handleResult(false, 0);
                });
            }
            return;
        }

        JSONObject packetData = new JSONObject();
        try {
            packetData.put("domain", AuthService.getDomain());
            packetData.put("touch", true);
        } catch (JSONException e) {
            LogUtils.w(TAG, "#getAuthDomain", e);
        }

        Packet requestPacket = new Packet(FerryServiceAction.Ping, packetData);
        this.pipeline.send(FerryService.NAME, requestPacket, new PipelineHandler() {
            @Override
            public void handleResponse(Packet packet) {
                if (packet.state.code != PipelineState.Ok.code) {
                    LogUtils.w(TAG, "#detectDomain - " + packet.state.code);
                    if (handler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            handler.handleResult(false, 0);
                        });
                    }
                    else {
                        execute(() -> {
                            handler.handleResult(false, 0);
                        });
                    }
                    return;
                }

                int stateCode = packet.extractServiceStateCode();
                if (stateCode != FerryServiceState.Ok.code) {
                    LogUtils.w(TAG, "#detectDomain - " + stateCode);
                    if (handler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            handler.handleResult(false, 0);
                        });
                    }
                    else {
                        execute(() -> {
                            handler.handleResult(false, 0);
                        });
                    }
                    return;
                }

                JSONObject data = packet.extractServiceData();
                AtomicLong duration = new AtomicLong(0);
                try {
                    houseOnline.set(data.getBoolean("online"));
                    duration.set(data.getLong("duration"));
                } catch (JSONException e) {
                    LogUtils.w(TAG, "#detectDomain", e);
                }

                // 成员身份
                membership = true;

                if (handler.isInMainThread()) {
                    executeOnMainThread(() -> {
                        handler.handleResult(houseOnline.get(), duration.get());
                    });
                }
                else {
                    execute(() -> {
                        handler.handleResult(houseOnline.get(), duration.get());
                    });
                }
            }
        });
    }

    /**
     * 获取指定名称的访问域。
     *
     * @param domainName 指定域名称。
     * @param successHandler 指定成功回调句柄。
     * @param failureHandler 指定失败回调句柄。
     */
    public void getDomain(String domainName, DomainHandler successHandler,
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

                    JSONObject infoJson = data.getJSONObject("info");
                    DomainInfo domainInfo = new DomainInfo(infoJson);

                    List<DomainMember> members = new ArrayList<>();
                    JSONArray array = data.getJSONArray("members");
                    for (int i = 0; i < array.length(); ++i) {
                        JSONObject memberJson = array.getJSONObject(i);
                        members.add(new DomainMember(memberJson));
                    }

                    // 保存数据
                    saveDomainInfo(domainInfo);

                    if (successHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            successHandler.handleDomain(authDomain, domainInfo, members);
                        });
                    }
                    else {
                        successHandler.handleDomain(authDomain, domainInfo, members);
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

    /**
     * 将当前联系人加入指定域。
     *
     * @param domainName 指定域名称。
     * @param successHandler 操作成功回调句柄。
     * @param failureHandler 操作失败回调句柄。
     */
    public void joinDomain(String domainName,
                           DomainMemberHandler successHandler,
                           FailureHandler failureHandler) {
        if (!this.pipeline.isReady()) {
            // 数据通道未就绪
            ModuleError error = new ModuleError(NAME, FerryServiceState.NoNetwork.code);
            execute(failureHandler, error);
            return;
        }

        ContactService contactService = (ContactService) this.kernel.getModule(ContactService.NAME);

        JSONObject packetData = new JSONObject();
        try {
            packetData.put("domain", domainName);
            packetData.put("contactId", contactService.getSelf().getId().longValue());
            packetData.put("way", JoinWay.QRCode.code);
        } catch (JSONException e) {
            LogUtils.w(TAG, "#joinDomain", e);
        }

        Packet requestPacket = new Packet(FerryServiceAction.JoinDomain, packetData);
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
                    AuthDomain authDomain = new AuthDomain(data.getJSONObject("authDomain"));
                    DomainInfo domainInfo = new DomainInfo(data.getJSONObject("domainInfo"));
                    DomainMember domainMember = new DomainMember(data.getJSONObject("member"));

                    if (successHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            successHandler.handleDomainMember(authDomain, domainInfo, domainMember);
                        });
                    }
                    else {
                        successHandler.handleDomainMember(authDomain, domainInfo, domainMember);
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

    /**
     * 使用邀请码将当前联系人加入指定域。
     *
     * @param invitationCode 指定邀请码。
     * @param successHandler 操作成功回调句柄。
     * @param failureHandler 操作失败回调句柄。
     */
    public void joinDomainByCode(String invitationCode,
                                 DomainMemberHandler successHandler,
                                 FailureHandler failureHandler) {
        if (!this.pipeline.isReady()) {
            // 数据通道未就绪
            ModuleError error = new ModuleError(NAME, FerryServiceState.NoNetwork.code);
            execute(failureHandler, error);
            return;
        }

        ContactService contactService = (ContactService) this.kernel.getModule(ContactService.NAME);

        JSONObject packetData = new JSONObject();
        try {
            packetData.put("invitationCode", invitationCode);
            packetData.put("contactId", contactService.getSelf().getId().longValue());
            packetData.put("way", JoinWay.InvitationCode.code);
        } catch (JSONException e) {
            LogUtils.w(TAG, "#joinDomainByCode", e);
        }

        Packet requestPacket = new Packet(FerryServiceAction.JoinDomain, packetData);
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
                    AuthDomain authDomain = new AuthDomain(data.getJSONObject("authDomain"));
                    DomainInfo domainInfo = new DomainInfo(data.getJSONObject("domainInfo"));
                    DomainMember domainMember = new DomainMember(data.getJSONObject("member"));

                    if (successHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            successHandler.handleDomainMember(authDomain, domainInfo, domainMember);
                        });
                    }
                    else {
                        successHandler.handleDomainMember(authDomain, domainInfo, domainMember);
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

    /**
     * 退出当前所在的域。
     *
     * @param activity 执行该操作的 Activity 实例。
     * @param successHandler 操作成功回调句柄。
     * @param failureHandler 操作失败回调句柄。
     */
    public void quitDomain(Activity activity, DomainMemberHandler successHandler,
                           FailureHandler failureHandler) {
        if (!this.pipeline.isReady()) {
            // 数据通道未就绪
            ModuleError error = new ModuleError(NAME, FerryServiceState.NoNetwork.code);
            execute(failureHandler, error);
            return;
        }

        if (!this.membership) {
            ModuleError error = new ModuleError(NAME, FerryServiceState.NoMember.code);
            execute(failureHandler, error);
            return;
        }

        ContactService contactService = (ContactService) this.kernel.getModule(ContactService.NAME);

        JSONObject packetData = new JSONObject();
        try {
            packetData.put("domain", AuthService.getDomain());
            packetData.put("contactId", contactService.getSelf().getId().longValue());
        } catch (JSONException e) {
            LogUtils.w(TAG, "#quitDomain", e);
        }

        Packet requestPacket = new Packet(FerryServiceAction.QuitDomain, packetData);
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

                // 更新状态
                membership = false;

                // 重置授权配置
                ((AuthService) kernel.getModule(AuthService.NAME)).resetAuthConfig(activity);

                JSONObject data = packet.extractServiceData();
                try {
                    AuthDomain authDomain = new AuthDomain(data.getJSONObject("authDomain"));
                    DomainInfo domainInfo = new DomainInfo(data.getJSONObject("domainInfo"));
                    DomainMember domainMember = new DomainMember(data.getJSONObject("member"));

                    if (successHandler.isInMainThread()) {
                        executeOnMainThread(() -> {
                            successHandler.handleDomainMember(authDomain, domainInfo, domainMember);
                            houseOnline.set(false);
                        });
                    }
                    else {
                        successHandler.handleDomainMember(authDomain, domainInfo, domainMember);
                        houseOnline.set(false);
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

    /**
     * 获取本地存储的域信息。
     *
     * @return
     */
    public DomainInfo getLocalDomainInfo() {
        return this.loadDomainInfo();
    }

    protected void takeOutTenets(TenetsHandler successHandler,
                                 FailureHandler failureHandler) {

    }

    private boolean saveDomainInfo(DomainInfo info) {
        File path = FileUtils.getFilePath(getContext(), "cube");
        File file = new File(path, "domain");
        return FileUtils.writeJSONFile(file, info.toJSON());
    }

    private DomainInfo loadDomainInfo() {
        DomainInfo info = null;

        File path = FileUtils.getFilePath(getContext(), "cube");
        File file = new File(path, "domain");
        try {
            JSONObject json = FileUtils.readJSONFile(file);
            info = new DomainInfo(json);
        } catch (JSONException e) {
            LogUtils.e(TAG, "#loadDomainInfo", e);
            return null;
        }

        return info;
    }

    protected void triggerOnline(Packet packet) {
        this.houseOnline.set(true);

        JSONObject data = packet.extractServiceData();
        try {
            String domainName = data.getString("domain");
            for (FerryEventListener listener : this.listeners) {
                executeOnMainThread(() -> {
                    listener.onFerryOnline(domainName);
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void triggerOffline(Packet packet) {
        this.houseOnline.set(false);

        JSONObject data = packet.extractServiceData();
        try {
            String domainName = data.getString("domain");
            for (FerryEventListener listener : this.listeners) {
                executeOnMainThread(() -> {
                    listener.onFerryOffline(domainName);
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
