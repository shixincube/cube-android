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
import android.net.Uri;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.shixincube.app.AppConsts;
import com.shixincube.app.R;
import com.shixincube.app.api.Explorer;
import com.shixincube.app.api.RetryWithDelay;
import com.shixincube.app.model.AppVersion;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.UIUtils;

import butterknife.BindView;
import cube.util.LogUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 关于界面。
 */
public class AboutActivity extends BaseActivity {

    @BindView(R.id.tvAppName)
    TextView appNameText;

    @BindView(R.id.btnCheckVersion)
    Button checkVersionButton;

    @BindView(R.id.btnGotoWebsite)
    Button gotoWebsiteButton;

    public AboutActivity() {
        super();
    }

    @Override
    public void initView() {
        this.setToolbarTitle(UIUtils.getString(R.string.about_cube));

        this.appNameText.setText(UIUtils.getString(R.string.app_name_in_about,
                AppConsts.VERSION));
    }

    @Override
    public void initListener() {
        this.checkVersionButton.setOnClickListener((view) -> {
            checkVersion();
        });

        this.gotoWebsiteButton.setOnClickListener((view) -> {
            Intent intent = new Intent();
            intent.setData(Uri.parse("https://shixincube.com"));
            intent.setAction(Intent.ACTION_VIEW);
            startActivity(intent);
        });
    }

    private void checkVersion() {
        showWaitingDialog(UIUtils.getString(R.string.tip_checking_version));

        Explorer.getInstance().getVersion()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> {
                    LogUtils.w("AboutActivity", "#checkVersion", error);

                    runOnUiThread(() -> {
                        hideWaitingDialog();

                        UIUtils.showToast(UIUtils.getString(R.string.tip_check_version_failed));
                    });
                })
                .retryWhen(new RetryWithDelay(3000, 1))
                .subscribe(versionResponse -> {
                    hideWaitingDialog();

                    AppVersion appVersion = versionResponse.version;
                    if (AppConsts.VERSION_MAJOR != appVersion.getMajor() ||
                        appVersion.isImportant()) {
                        // 必须更新
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(UIUtils.getString(R.string.check_version));
                        builder.setMessage(UIUtils.getString(R.string.tip_important_version_format,
                                appVersion.getDescription()));
                        builder.setPositiveButton(UIUtils.getString(R.string.sure), (dialogInterface, which) -> {
                            downloadApkFile(appVersion.getDescription());
                        });
                        builder.show();
                    }
                    else if (AppConsts.VERSION_MINOR != appVersion.getMinor() ||
                        AppConsts.VERSION_REVISION != appVersion.getRevision()) {
                        // 建议更新
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(UIUtils.getString(R.string.check_version));
                        builder.setMessage(UIUtils.getString(R.string.tip_new_version_format,
                                appVersion.getDescription()));
                        builder.setNegativeButton(UIUtils.getString(R.string.cancel), null);
                        builder.setPositiveButton(UIUtils.getString(R.string.sure), (dialogInterface, which) -> {
                            downloadApkFile(appVersion.getDescription());
                        });
                        builder.show();
                    }
                    else {
                        // 版本一致
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(UIUtils.getString(R.string.check_version));
                        builder.setMessage(UIUtils.getString(R.string.tip_no_new_version));
                        builder.setNegativeButton(UIUtils.getString(R.string.close), null);
                        builder.show();
                    }
                });
    }

    private void downloadApkFile(String version) {
        String url = AppConsts.APP_DOWNLOAD_URL + "Cube_release_" + version + ".apk";

    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_about;
    }
}
