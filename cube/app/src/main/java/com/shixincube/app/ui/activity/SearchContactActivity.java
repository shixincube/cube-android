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

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.presenter.SearchContactPresenter;
import com.shixincube.app.ui.view.SearchContactView;

import butterknife.BindView;

/**
 * 搜索联系人。
 */
public class SearchContactActivity extends BaseActivity<SearchContactView, SearchContactPresenter> implements SearchContactView {

    @BindView(R.id.llToolbarSearch)
    LinearLayout toolbarSearchLayout;
    @BindView(R.id.etSearchContent)
    EditText searchContactText;

    @BindView(R.id.rlNoResultTip)
    RelativeLayout noResultLayout;

    @BindView(R.id.llSearch)
    LinearLayout searchLayout;
    @BindView(R.id.tvContent)
    TextView contentTextView;

    public SearchContactActivity() {
    }

    @Override
    public void initView() {
        this.toolbarTitle.setVisibility(View.GONE);
        this.toolbarSearchLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void initListener() {
        this.searchContactText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String content = searchContactText.getText().toString().trim();
                noResultLayout.setVisibility(View.GONE);
                if (!TextUtils.isEmpty(content)) {
                    searchLayout.setVisibility(View.VISIBLE);
                    contentTextView.setText(content);
                }
                else {
                    searchLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        this.searchLayout.setOnClickListener((view) -> {
            presenter.searchContact();
        });
    }

    @Override
    protected SearchContactPresenter createPresenter() {
        return new SearchContactPresenter(this);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_search_contact;
    }

    @Override
    public EditText getSearchContentText() {
        return this.searchContactText;
    }

    @Override
    public RelativeLayout getNoResultLayout() {
        return this.noResultLayout;
    }

    @Override
    public LinearLayout getSearchLayout() {
        return this.searchLayout;
    }
}
