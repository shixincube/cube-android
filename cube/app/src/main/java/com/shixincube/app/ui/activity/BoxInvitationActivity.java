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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Button;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.BubbleImageView;

import java.io.File;

import butterknife.BindView;
import cube.core.handler.FileHandler;
import cube.engine.CubeEngine;
import cube.ferry.handler.DomainInfoHandler;
import cube.ferry.model.DomainInfo;

/**
 * 盒子邀请码界面。
 */
public class BoxInvitationActivity extends BaseActivity {

    @BindView(R.id.bivQRCode)
    BubbleImageView qrCodeImageView;

    @BindView(R.id.tvIC1)
    TextView ic1View;
    @BindView(R.id.tvIC2)
    TextView ic2View;
    @BindView(R.id.tvIC3)
    TextView ic3View;
    @BindView(R.id.tvIC4)
    TextView ic4View;
    @BindView(R.id.tvIC5)
    TextView ic5View;
    @BindView(R.id.tvIC6)
    TextView ic6View;

    @BindView(R.id.btnCopyCode)
    Button copyCodeButton;

    private String invitationCode;

    public BoxInvitationActivity() {
        super();
    }

    @Override
    public void initView() {
        this.setToolbarTitle(UIUtils.getString(R.string.box_invitation_code));
    }

    @Override
    public void initListener() {
        this.copyCodeButton.setOnClickListener(view -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData data = ClipData.newPlainText("Code",
                    UIUtils.getString(R.string.clipboard_invitation_code_text_format, invitationCode));
            cm.setPrimaryClip(data);

            UIUtils.showToast(UIUtils.getString(R.string.toast_copy_invitation_code));
        });
    }

    @Override
    public void initData() {
        CubeEngine.getInstance().getFerryService().getDomainInfo(new DomainInfoHandler() {
            @Override
            public void handleDomainInfo(DomainInfo domainInfo) {
                invitationCode = domainInfo.getInvitationCode();
                ic1View.setText(invitationCode.substring(0, 1));
                ic2View.setText(invitationCode.substring(1, 2));
                ic3View.setText(invitationCode.substring(2, 3));
                ic4View.setText(invitationCode.substring(3, 4));
                ic5View.setText(invitationCode.substring(4, 5));
                ic6View.setText(invitationCode.substring(5, 6));
            }
        });

        CubeEngine.getInstance().getFerryService().getDomainQRCodeFile(new FileHandler() {
            @Override
            public void handleFile(File file) {
                if (null != file) {
                    int size = UIUtils.dp2px(240);
                    Glide.with(BoxInvitationActivity.this).load(file)
                            .override(size, size)
                            .centerCrop()
                            .into(qrCodeImageView);
                    qrCodeImageView.showShadow(false);
                }
            }
        });
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_box_invitation;
    }
}
