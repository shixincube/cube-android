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
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.shixincube.app.AppConsts;
import com.shixincube.app.R;
import com.shixincube.app.api.Explorer;
import com.shixincube.app.api.RetryWithDelay;
import com.shixincube.app.model.AppVersion;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.FileUtils;
import com.shixincube.app.util.UIUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;

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
                        builder.setCancelable(false);
                        builder.setTitle(UIUtils.getString(R.string.check_version));
                        builder.setMessage(UIUtils.getString(R.string.tip_important_version_format,
                                appVersion.getDescription()));
                        builder.setPositiveButton(UIUtils.getString(R.string.sure), (dialogInterface, which) -> {
                            downloadApkFile(appVersion.getDescription(), false);
                        });
                        builder.show();
                    }
                    else if (AppConsts.VERSION_MINOR != appVersion.getMinor() ||
                        AppConsts.VERSION_REVISION != appVersion.getRevision()) {
                        // 建议更新
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setCancelable(false);
                        builder.setTitle(UIUtils.getString(R.string.check_version));
                        builder.setMessage(UIUtils.getString(R.string.tip_new_version_format,
                                appVersion.getDescription()));
                        builder.setNegativeButton(UIUtils.getString(R.string.cancel), null);
                        builder.setPositiveButton(UIUtils.getString(R.string.sure), (dialogInterface, which) -> {
                            downloadApkFile(appVersion.getDescription(), true);
                        });
                        builder.show();
                    }
                    else {
                        // 版本一致
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setCancelable(false);
                        builder.setTitle(UIUtils.getString(R.string.check_version));
                        builder.setMessage(UIUtils.getString(R.string.tip_no_new_version));
                        builder.setNegativeButton(UIUtils.getString(R.string.close), null);
                        builder.show();
                    }
                });
    }

    private void downloadApkFile(String version, boolean canCancel) {
        // 初始化界面
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(UIUtils.getString(R.string.downloading));
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_progress, null);
        final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.pbProgress);
        view.findViewById(R.id.tvDescription).setVisibility(View.GONE);
        final TextView totalSizeView = (TextView) view.findViewById(R.id.tvTotalSize);
        totalSizeView.setText("0 KB");
        final TextView progressSizeView = (TextView) view.findViewById(R.id.tvProgressSize);
        progressSizeView.setText("0 KB");
        builder.setView(view);

        DecimalFormat format = new DecimalFormat("###,###,##0.00");
        float denominator = 1024.0f * 1024.0f;

        // 取消下载操作
        AtomicBoolean cancel = new AtomicBoolean(false);
        builder.setNegativeButton(UIUtils.getString(R.string.cancel), (dialog, which) -> {
            dialog.dismiss();
            cancel.set(true);
            UIUtils.showToast(UIUtils.getString(R.string.downloading_stopped));
        });
        AlertDialog downloadingDialog = builder.show();

        (new Thread(() -> {
            String dir = FileUtils.getDir("");
            String filePath = dir + "Cube_release_" + version + ".apk";
            File file = new File(filePath);
            if (file.exists() && file.length() > 0) {
                file.delete();
            }

            String urlString = AppConsts.APP_DOWNLOAD_URL + "Cube_release_" + version + ".apk";
            try {
                URL url = new URL(urlString);
                FileUtils.downloadFile(url, file, new FileUtils.DownloadListener() {
                    @Override
                    public void onStarted(URL url, File file, long fileSize) {
                        LogUtils.d("AboutActivity", "#onStarted - download: " + url.toString());
                        runOnUiThread(() -> {
                            totalSizeView.setText(format.format((float)fileSize / denominator) + " MB");
                        });
                    }

                    @Override
                    public boolean onDownloading(URL url, File file, long totalSize, long processedSize) {
                        if (cancel.get()) {
                            return false;
                        }

                        // 计算进度
                        int progress = (int) (((float) processedSize / (float)totalSize) * 100.0f);
                        runOnUiThread(() -> {
                            progressSizeView.setText(format.format((float)processedSize / denominator) + " MB");
                            progressBar.setProgress(progress);
                        });

                        return true;
                    }

                    @Override
                    public void onCompleted(URL url, File file) {
                        LogUtils.i("AboutActivity", "#onCompleted - download to: " + file.getAbsolutePath());

                        if (file.exists() && file.length() > 0) {
                            runOnUiThread(() -> {
                                downloadingDialog.dismiss();
                                showInstallPrompt(file, canCancel);
                            });
                        }
                    }
                });
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        })).start();
    }

    private void showInstallPrompt(File file, boolean canCancel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(UIUtils.getString(R.string.install_update_file));
        builder.setMessage(UIUtils.getString(R.string.file_download_completed));
        if (canCancel) {
            builder.setNegativeButton(R.string.cancel, null);
        }
        builder.setPositiveButton(R.string.install_now, (dialog, witch) -> {
            installAPK(file);
        });
        builder.show();
    }

    private void installAPK(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Uri uri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            uri = FileProvider.getUriForFile(this,this.getPackageName() + ".provider", file);
        }
        else {
            uri = Uri.fromFile(file);
        }

        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        this.startActivity(intent);
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
