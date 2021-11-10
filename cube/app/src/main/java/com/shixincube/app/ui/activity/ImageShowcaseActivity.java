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
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.bm.library.PhotoView;
import com.bumptech.glide.Glide;
import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;

import butterknife.BindView;

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

    private String fileName;
    private String fileURL;

//    private FrameLayout

    public ImageShowcaseActivity() {
        super();
    }

    @Override
    public void init() {
        this.fileName = getIntent().getStringExtra("name");
        this.fileURL = getIntent().getStringExtra("url");
    }

    @Override
    public void initView() {
        setToolbarTitle(this.fileName);
        toolbarMoreButton.setVisibility(View.VISIBLE);

        this.photoView.enable();

        Glide.with(this)
                .load(Uri.parse(this.fileURL))
                .placeholder(R.mipmap.default_image)
                .centerCrop()
                .into(this.photoView);
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
