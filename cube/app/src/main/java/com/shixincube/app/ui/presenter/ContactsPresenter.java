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

package com.shixincube.app.ui.presenter;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.ContactsView;
import com.shixincube.app.widget.adapter.AdapterForRecyclerView;
import com.shixincube.app.widget.adapter.HeaderAndFooterAdapter;
import com.shixincube.app.widget.adapter.ViewHolderForRecyclerView;

import java.util.ArrayList;
import java.util.List;

import cube.contact.model.Contact;

/**
 * 联系人清单。
 */
public class ContactsPresenter extends BasePresenter<ContactsView> {

    private List<Contact> contacts;

    private HeaderAndFooterAdapter adapter;

    public ContactsPresenter(BaseActivity activity) {
        super(activity);
        this.contacts = new ArrayList<>();
    }

    public void loadContacts() {
        this.setAdapter();
    }

    private void setAdapter() {
        if (null == this.adapter) {
            AdapterForRecyclerView adapter = new AdapterForRecyclerView<Contact>(activity, this.contacts, R.layout.item_contact) {
                @Override
                public void convert(ViewHolderForRecyclerView helper, Contact item, int position) {

                }
            };

            adapter.addHeaderView(getView().getHeaderView());
            adapter.addFooterView(getView().getFooterView());
            this.adapter = adapter.getHeaderAndFooterAdapter();
            getView().getContactsView().setAdapter(this.adapter);

            // 设置事件监听

        }
    }

    private 
}
