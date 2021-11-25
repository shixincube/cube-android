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

import android.content.Intent;
import android.view.View;

import com.shixincube.app.R;
import com.shixincube.app.api.Explorer;
import com.shixincube.app.api.StateCode;
import com.shixincube.app.manager.AccountHelper;
import com.shixincube.app.model.Account;
import com.shixincube.app.ui.activity.ContactDetailsActivity;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.SearchContactView;
import com.shixincube.app.util.RegularUtils;
import com.shixincube.app.util.UIUtils;

import cube.contact.model.Contact;
import cube.engine.CubeEngine;
import cube.util.LogUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 搜索联系人。
 */
public class SearchContactPresenter extends BasePresenter<SearchContactView> {

    public SearchContactPresenter(BaseActivity activity) {
        super(activity);
    }

    public void searchContact() {
        String content = getView().getSearchContentText().getText().toString().trim();

        if (RegularUtils.isMobile(content)) {
            activity.showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

            // 作为手机号码进行搜索
            Explorer.getInstance().searchAccount(content)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((searchAccountResultResponse) -> {
                        activity.hideWaitingDialog();

                        if (searchAccountResultResponse.code == StateCode.Success) {
                            handleSuccess(searchAccountResultResponse.account.toAccount());
                        }
                        else {
                            getView().getNoResultLayout().setVisibility(View.VISIBLE);
                            getView().getSearchLayout().setVisibility(View.GONE);
                        }
                    }, this::handleError);
        }
        else if (RegularUtils.isNumeric(content)) {
            activity.showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

            // 作为魔方号进行搜索
            Explorer.getInstance().searchAccountById(Long.parseLong(content))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((searchAccountResultResponse) -> {
                        activity.hideWaitingDialog();

                        if (searchAccountResultResponse.code == StateCode.Success) {
                            handleSuccess(searchAccountResultResponse.account.toAccount());
                        }
                        else {
                            getView().getNoResultLayout().setVisibility(View.VISIBLE);
                            getView().getSearchLayout().setVisibility(View.GONE);
                        }
                    }, this::handleError);
        }
        else {
            UIUtils.showToast(UIUtils.getString(R.string.search_condition_error));
        }
    }

    private void handleSuccess(Account account) {
        Contact contact = AccountHelper.getInstance().createContact(account);

        CubeEngine.getInstance().getContactService().saveLocalContact(contact);

        // 判断是否是自己的联系人
        boolean isMyContact = false;
        if (CubeEngine.getInstance().getContactService().getDefaultContactZone().contains(contact)) {
            isMyContact = true;
        }

        Intent intent = new Intent(activity, ContactDetailsActivity.class);
        intent.putExtra("contactId", contact.getId());
        intent.putExtra("isMyContact", isMyContact);
        activity.jumpToActivity(intent);
    }

    private void handleError(Throwable throwable) {
        activity.hideWaitingDialog();
        LogUtils.e(throwable);
    }
}
