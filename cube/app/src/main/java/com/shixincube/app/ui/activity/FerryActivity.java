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

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;

import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.presenter.FerryPresenter;
import com.shixincube.app.ui.view.FerryView;

import butterknife.BindView;
import cube.engine.CubeEngine;

/**
 * Ferry 模式检查。
 */
public class FerryActivity extends BaseActivity<FerryView, FerryPresenter> implements FerryView {

    private final static String TAG = FerryActivity.class.getSimpleName();

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

    public FerryActivity() {
        super();
    }

    @Override
    public void init() {
        if (CubeEngine.getInstance().getConfig().domain.equals("shixincube.com")) {
            this.validBox = false;
        }
        else {
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
            presenter.processQRCodeResult("cube://domain.demo-ferryhouse-cube");
//            ScanOptions options = new ScanOptions();
//            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
//            options.setCameraId(0);
//            options.setOrientationLocked(true);
//            options.setBeepEnabled(true);
//            options.setPrompt(UIUtils.getString(R.string.scan_qr_prompt));
//            barcodeLauncher.launch(options);
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
