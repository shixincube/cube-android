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

import android.widget.LinearLayout;
import android.widget.TextView;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.presenter.AddContactPresenter;
import com.shixincube.app.ui.view.AddContactView;
import com.shixincube.app.util.UIUtils;

import butterknife.BindView;
import cube.engine.CubeEngine;

/**
 * 添加联系人。
 */
public class AddContactActivity extends BaseActivity<AddContactView, AddContactPresenter> implements AddContactView {

    @BindView(R.id.llSearchContact)
    LinearLayout searchContactLayout;

    @BindView(R.id.tvCubeId)
    TextView cubeIdTextView;

    public AddContactActivity() {
        super();
    }

    @Override
    public void initView() {
        setToolbarTitle(UIUtils.getString(R.string.add_contact));
        this.cubeIdTextView.setText(CubeEngine.getInstance().getContactService().getSelf().getId().toString());
    }

    @Override
    public void initListener() {
        this.searchContactLayout.setOnClickListener((view) -> {
            jumpToActivity(SearchContactActivity.class);
        });
    }

    @Override
    protected AddContactPresenter createPresenter() {
        return new AddContactPresenter(this);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_add_contact;
    }
}
