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

package com.shixincube.app.ui.fragment;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.shixincube.app.R;
import com.shixincube.app.ui.activity.MainActivity;
import com.shixincube.app.ui.base.BaseFragment;
import com.shixincube.app.ui.presenter.ContactsPresenter;
import com.shixincube.app.ui.view.ContactsView;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.QuickIndexBar;
import com.shixincube.app.widget.recyclerview.RecyclerView;

import butterknife.BindView;

/**
 * 联系人清单。
 */
public class ContactsFragment extends BaseFragment<ContactsView, ContactsPresenter> implements ContactsView {

    @BindView(R.id.rvContacts)
    RecyclerView contactsView;

    @BindView(R.id.qib)
    QuickIndexBar indexBar;

    @BindView(R.id.tvLetter)
    TextView letterView;

    private View headerView;

    private TextView footerView;

    public ContactsFragment() {
        super();
    }

    @Override
    public void initView(View rootView) {
        this.headerView = View.inflate(getActivity(), R.layout.header_contacts, null);
        this.footerView = new TextView(getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                UIUtils.dp2px(getContext(), 50));
        this.footerView.setLayoutParams(params);
        this.footerView.setGravity(Gravity.CENTER);
    }

    @Override
    public void initData() {
        this.presenter.loadContacts();
    }

    @Override
    protected ContactsPresenter createPresenter() {
        return new ContactsPresenter((MainActivity) getActivity());
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.fragment_contacts;
    }

    @Override
    public RecyclerView getContactsView() {
        return this.contactsView;
    }

    @Override
    public View getHeaderView() {
        return this.headerView;
    }

    @Override
    public TextView getFooterView() {
        return this.footerView;
    }
}
