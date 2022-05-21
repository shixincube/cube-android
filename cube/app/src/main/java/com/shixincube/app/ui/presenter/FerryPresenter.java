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

package com.shixincube.app.ui.presenter;

import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import com.shixincube.app.R;
import com.shixincube.app.manager.AccountHelper;
import com.shixincube.app.manager.CubeConnection;
import com.shixincube.app.ui.activity.MainActivity;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.FerryView;
import com.shixincube.app.util.UIUtils;

import java.util.List;

import cube.auth.model.AuthDomain;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;
import cube.engine.util.CodeUtils;
import cube.engine.util.Future;
import cube.engine.util.Promise;
import cube.engine.util.PromiseFuture;
import cube.engine.util.PromiseHandler;
import cube.ferry.FerryService;
import cube.ferry.handler.DefaultDomainMemberHandler;
import cube.ferry.handler.DomainHandler;
import cube.ferry.model.DomainMember;
import cube.util.LogUtils;

/**
 * Ferry Presenter
 */
public class FerryPresenter extends BasePresenter<FerryView> {

    private final static String TAG = FerryPresenter.class.getSimpleName();

    public FerryPresenter(BaseActivity activity) {
        super(activity);
    }

    public void processQRCodeResult(String string) {
        LogUtils.d(TAG, "QR code: " + string);

        if (string.length() < 16) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(UIUtils.getString(R.string.prompt));
            builder.setMessage(UIUtils.getString(R.string.unrecognized_code));
            builder.setNegativeButton(UIUtils.getString(R.string.sure), null);
            builder.show();
            return;
        }

        String protocol = CodeUtils.extractProtocol(string);
        if (!protocol.equalsIgnoreCase("cube")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(UIUtils.getString(R.string.prompt));
            builder.setMessage(UIUtils.getString(R.string.unrecognized_code));
            builder.setNegativeButton(UIUtils.getString(R.string.sure), null);
            builder.show();
            return;
        }

        String[] data = CodeUtils.extractResourceSegments(string);
        if (null == data || data.length != 2) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(UIUtils.getString(R.string.prompt));
            builder.setMessage(UIUtils.getString(R.string.unsupported_qr_format));
            builder.setNegativeButton(UIUtils.getString(R.string.sure), null);
            builder.show();
            return;
        }

        if (!data[0].equalsIgnoreCase("domain")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(UIUtils.getString(R.string.prompt));
            builder.setMessage(UIUtils.getString(R.string.unsupported_qr_format));
            builder.setNegativeButton(UIUtils.getString(R.string.sure), null);
            builder.show();
        }

        // 域名称
        String domainName = data[1];

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(UIUtils.getString(R.string.prompt));
        builder.setMessage(UIUtils.getString(R.string.ferry_tip_join_domain, domainName));
        builder.setPositiveButton(UIUtils.getString(R.string.sure), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                // 提示等待
                activity.showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

                FerryService service = CubeEngine.getInstance().getFerryService();
                service.getAuthDomain(domainName, new DomainHandler() {
                    @Override
                    public void handleDomain(AuthDomain authDomain, List<DomainMember> members) {
                        // 加入域
                        service.joinDomain(domainName,
                                AccountHelper.getInstance().getCurrentAccount().id,
                                new DefaultDomainMemberHandler() {
                                    @Override
                                    public void handleDomainMember(DomainMember member) {
                                        // 重置域
                                        resetDomain(authDomain, members);
                                    }
                                }, new DefaultFailureHandler(true) {
                                    @Override
                                    public void handleFailure(Module module, ModuleError error) {
                                        // 隐藏对话框
                                        activity.hideWaitingDialog();

                                        UIUtils.showToast(UIUtils.getString(R.string.ferry_join_domain_failed));

                                        LogUtils.d(TAG, "#joinDomain - " + error.code);
                                    }
                                });
                    }

                    @Override
                    public boolean isInMainThread() {
                        return false;
                    }
                }, new DefaultFailureHandler(true) {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {
                        // 隐藏对话框
                        activity.hideWaitingDialog();

                        UIUtils.showToast(UIUtils.getString(R.string.ferry_get_domain_error));
                    }
                });
            }
        });
        builder.setNegativeButton(UIUtils.getString(R.string.cancel), null);
        builder.show();
    }

    private void resetDomain(AuthDomain authDomain, List<DomainMember> memberList) {
        Promise.create(new PromiseHandler<AuthDomain>() {
            @Override
            public void emit(PromiseFuture<AuthDomain> promise) {
                CubeConnection connection = new CubeConnection();
                connection.setSuccessHandler(() -> {
                    promise.resolve(authDomain);
                });
                connection.setFailureHandler(() -> {
                    promise.reject(authDomain);
                });

                if (!CubeEngine.getInstance().resetConfig(activity, authDomain, connection)) {
                    // 写配置文件失败
                    promise.reject(authDomain);
                }
            }
        }).thenOnMainThread(new Future<AuthDomain>() {
            @Override
            public void come(AuthDomain data) {
                // 隐藏对话框
                activity.hideWaitingDialog();

                // 跳转到主界面
                activity.jumpToActivityAndClearTask(MainActivity.class);
            }
        }).catchRejectOnMainThread(new Future<AuthDomain>() {
            @Override
            public void come(AuthDomain data) {
                // 隐藏对话框
                activity.hideWaitingDialog();

                UIUtils.showToast(UIUtils.getString(R.string.ferry_join_domain_failed));

                LogUtils.d(TAG, "#resetDomain - Reset engine failed: "
                        + authDomain.domainName);
            }
        }).launch();
    }
}
