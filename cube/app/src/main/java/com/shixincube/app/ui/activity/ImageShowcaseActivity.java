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

import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bm.library.PhotoView;
import com.bumptech.glide.Glide;
import com.shixincube.app.AppConsts;
import com.shixincube.app.R;
import com.shixincube.app.api.Explorer;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.FileUtils;
import com.shixincube.app.util.PopupWindowUtils;
import com.shixincube.app.util.UIUtils;

import java.io.File;

import butterknife.BindView;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;
import cube.filestorage.model.FileAnchor;
import cube.messaging.handler.DefaultLoadAttachmentHandler;
import cube.messaging.model.FileAttachment;
import cube.messaging.model.Message;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 图像展示界面。
 */
public class ImageShowcaseActivity extends BaseActivity {

    @BindView(R.id.ibToolbarMore)
    ImageButton toolbarMoreButton;

    @BindView(R.id.pv)
    PhotoView photoView;
    @BindView(R.id.pb)
    ProgressBar progressBar;

    private FrameLayout menuView;
    private PopupWindow popupWindow;

    private long messageId;
    private String fileName;
    private String fileURL;
    private String rawFileURL;

    public ImageShowcaseActivity() {
        super();
    }

    @Override
    public void init() {
        this.messageId = getIntent().getLongExtra("messageId", 0);
        this.fileName = getIntent().getStringExtra("name");
        this.fileURL = getIntent().getStringExtra("url");
        this.rawFileURL = getIntent().getStringExtra("raw");
    }

    @Override
    public void initView() {
        setToolbarTitle(this.fileName);

        if (!this.fileURL.startsWith("file")) {
            this.toolbarMoreButton.setVisibility(View.VISIBLE);
        }
        else {
            this.toolbarMoreButton.setVisibility(View.GONE);
        }

        this.photoView.enable();

        if (null != this.rawFileURL) {
            if (this.rawFileURL.startsWith("file:")) {
                Glide.with(this)
                        .load(Uri.parse(this.rawFileURL))
                        .placeholder(R.mipmap.default_image)
                        .centerCrop()
                        .into(this.photoView);
            }
            else {
                if (this.messageId > 0) {
                    Message message = CubeEngine.getInstance().getMessagingService().getMessageById(this.messageId);
                    message.loadAttachment(new DefaultLoadAttachmentHandler(true) {
                        @Override
                        public void handleLoading(Message message, FileAttachment fileAttachment, FileAnchor fileAnchor) {
                            // Nothing
                        }

                        @Override
                        public void handleLoaded(Message message, FileAttachment fileAttachment) {
                            Glide.with(ImageShowcaseActivity.this)
                                    .load(Uri.parse(fileAttachment.getPrefFileURL()))
                                    .placeholder(R.mipmap.default_image)
                                    .centerCrop()
                                    .into(photoView);
                        }
                    }, new DefaultFailureHandler(true) {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            Glide.with(ImageShowcaseActivity.this)
                                    .load(Uri.parse(rawFileURL))
                                    .placeholder(R.mipmap.default_image)
                                    .centerCrop()
                                    .into(photoView);
                        }
                    });
                }
                else {
                    Glide.with(this)
                            .load(Uri.parse(this.rawFileURL))
                            .placeholder(R.mipmap.default_image)
                            .centerCrop()
                            .into(this.photoView);
                }
            }
        }
        else {
            Glide.with(this)
                    .load(Uri.parse(this.fileURL))
                    .placeholder(R.mipmap.default_image)
                    .centerCrop()
                    .into(this.photoView);
        }
    }

    @Override
    public void initListener() {
        this.toolbarMoreButton.setOnClickListener((view) -> {
            showPopupMenu();
        });
    }

    private void showPopupMenu() {
        if (null == this.menuView) {
            this.menuView = new FrameLayout(this);
            this.menuView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            this.menuView.setBackgroundColor(UIUtils.getColor(R.color.white));

            TextView item = new TextView(this);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, UIUtils.dp2px(50));
            item.setLayoutParams(params);
            item.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            item.setPadding(UIUtils.dp2px(20), 0, 0, 0);
            item.setTextColor(UIUtils.getColor(R.color.gray0));
            item.setTextSize(14);
            item.setText(UIUtils.getString(R.string.save_to_phone));
            this.menuView.addView(item);

            item.setOnClickListener((view) -> {
                if (this.fileURL.startsWith("file")) {
                    File source = new File(Uri.parse(fileURL).getPath());
                    File target = new File(AppConsts.IMAGE_DIR, fileName);
                    boolean result = FileUtils.copy(source, target);
                    UIUtils.showToast(result ? UIUtils.getString(R.string.save_success) :
                            UIUtils.getString(R.string.save_failure));
                    popupWindow.dismiss();
                    popupWindow = null;
                }
                else {
                    // 下载文件
                    File target = new File(AppConsts.IMAGE_DIR, fileName);
                    Explorer.getInstance().getAppInterface()
                            .download(fileURL)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((responseBody -> {
                                boolean result = FileUtils.save(target, responseBody);
                                UIUtils.showToast(result ? UIUtils.getString(R.string.save_success) :
                                        UIUtils.getString(R.string.save_failure));
                                popupWindow.dismiss();
                                popupWindow = null;
                            }));
                }
            });
        }

        this.popupWindow = PopupWindowUtils.getPopupWindowAtLocation(menuView, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, getWindow().getDecorView().getRootView(), Gravity.BOTTOM, 0, 0);
        this.popupWindow.setOnDismissListener(() -> {
            PopupWindowUtils.makeWindowLight(ImageShowcaseActivity.this);
        });
        PopupWindowUtils.makeWindowDark(ImageShowcaseActivity.this);
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_image_showcase;
    }
}
