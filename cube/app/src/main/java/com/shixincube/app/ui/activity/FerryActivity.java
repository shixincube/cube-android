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

package com.shixincube.app.ui.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.shixincube.app.CubeBaseApp;
import com.shixincube.app.R;
import com.shixincube.app.api.Explorer;
import com.shixincube.app.manager.AccountHelper;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.presenter.FerryPresenter;
import com.shixincube.app.ui.view.FerryView;
import com.shixincube.app.util.DeviceUtils;
import com.shixincube.app.util.UIUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import cube.contact.ContactService;
import cube.contact.handler.DefaultSignHandler;
import cube.contact.model.Self;
import cube.core.ModuleError;
import cube.engine.CubeEngine;
import cube.util.LogUtils;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Ferry 模式检查。
 */
public class FerryActivity extends BaseActivity<FerryView, FerryPresenter> implements FerryView {

    private final static String TAG = FerryActivity.class.getSimpleName();

    public final static String EXTRA_MEMBERSHIP = "membership";

    private boolean validBox = true;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(), result -> {
                if (result.getContents() == null) {
                    Intent originalIntent = result.getOriginalIntent();
                    if (originalIntent == null) {
                        Log.d(TAG, "Cancelled scan");
                        Toast.makeText(FerryActivity.this, "已取消", Toast.LENGTH_LONG).show();
                    }
                    else if (originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
                        Log.d(TAG, "Cancelled scan due to missing camera permission");
                        Toast.makeText(FerryActivity.this,
                                "没有获得摄像机权限，已取消扫码", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Log.d(TAG, "Scanned");
                    presenter.processQRCodeResult(result.getContents());
                    //Toast.makeText(FerryActivity.this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                }
            });

    @BindView(R.id.llJumping)
    View llJumpingView;

    @BindView(R.id.llFerry)
    View llFerryView;

    @BindView(R.id.btnScanQR)
    Button btnScanQRCode;

    @BindView(R.id.btnInput)
    Button btnInputInvitation;

    @BindView(R.id.btnLogout)
    Button btnLogout;

    public FerryActivity() {
        super();
    }

    @Override
    public void init() {
        LogUtils.d(TAG, "#init");

        if (CubeEngine.getInstance().getConfig().domain.equals("shixincube.com")) {
            this.validBox = false;
        }
        else {
            if (this.getIntent().hasExtra(EXTRA_MEMBERSHIP)) {
                if (!this.getIntent().getBooleanExtra(EXTRA_MEMBERSHIP, false)) {
                    this.validBox = false;
                    return;
                }
            }

            // 跳转到 MainActivity
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    jumpToActivityAndClearTask(MainActivity.class);
                }
            });
        }
    }

    @Override
    public void initView() {
        if (this.validBox) {
            this.llJumpingView.setVisibility(View.VISIBLE);
            this.llFerryView.setVisibility(View.GONE);
        }
        else {
            this.llJumpingView.setVisibility(View.GONE);
            this.llFerryView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void initListener() {
        this.btnScanQRCode.setOnClickListener((view) -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setCameraId(0);
            options.setOrientationLocked(true);
            options.setBeepEnabled(true);
            options.setPrompt(UIUtils.getString(R.string.scan_qr_prompt));
            barcodeLauncher.launch(options);
        });

        this.btnInputInvitation.setOnClickListener((view) -> {
            Intent intent = new Intent(this, InvitationCodeActivity.class);
            startActivityForResult(intent, InvitationCodeActivity.RESULT);
        });

        this.btnLogout.setOnClickListener((view) -> {
            logout();
        });
    }

    @Override
    protected FerryPresenter createPresenter() {
        return new FerryPresenter(this);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_ferry;
    }

    @Override
    public Button getScanQRButton() {
        return this.btnScanQRCode;
    }

    @Override
    public Button getInputInvitationButton() {
        return this.btnInputInvitation;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == InvitationCodeActivity.RESULT && null != data) {
            String code = data.getStringExtra(InvitationCodeActivity.EXTRA_CODE);
            this.presenter.processInvitationCode(code);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    private void logout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(UIUtils.getString(R.string.prompt));
        builder.setMessage(UIUtils.getString(R.string.do_you_want_to_logout));
        builder.setPositiveButton(UIUtils.getString(R.string.sure), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                showWaitingDialog(UIUtils.getString(R.string.logout));

                // 删除当前账号
                AccountHelper.getInstance().removeCurrentAccount();
                // 删除本地令牌
                String tokenCode = AccountHelper.getInstance().deleteToken();
                if (null == tokenCode) {
                    LogUtils.w(TAG, "Token is null");
                    return;
                }

                AtomicBoolean finishLogout = new AtomicBoolean(false);
                AtomicBoolean finishSignOut = new AtomicBoolean(false);

                Runnable jumpTask = new Runnable() {
                    @Override
                    public void run() {
                        if (finishLogout.get() && finishSignOut.get()) {
                            CubeBaseApp.getMainThreadHandler().post(() -> {
                                jumpToActivityAndClearTask(SplashActivity.class);
                            });
                        }
                    }
                };

                // 从应用服务器登出
                String device = DeviceUtils.getDeviceDescription(getApplicationContext());
                Explorer.getInstance().logout(tokenCode, device)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .doOnError(error -> {
                            LogUtils.w(TAG, "Logout failed", error);
                            finishLogout.set(true);
                            jumpTask.run();
                        })
                        .subscribe(logoutResponse -> {
                            LogUtils.i(TAG, "Logout - " + logoutResponse.code);
                            finishLogout.set(true);
                            jumpTask.run();
                        });

                // 从引擎签出
                CubeEngine.getInstance().signOut(new DefaultSignHandler(false) {
                    @Override
                    public void handleSuccess(ContactService service, Self self) {
                        LogUtils.i(TAG, "SignOut : " + self.id);
                        finishSignOut.set(true);
                        jumpTask.run();
                    }

                    @Override
                    public void handleFailure(ContactService service, ModuleError error) {
                        LogUtils.w(TAG, "SignOut failed : " + error.code);
                        finishSignOut.set(true);
                        jumpTask.run();
                    }
                });
            }
        });
        builder.setNegativeButton(UIUtils.getString(R.string.cancel), null);
        builder.show();
    }
}
