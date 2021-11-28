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
import com.shixincube.app.api.Explorer;
import com.shixincube.app.manager.AccountHelper;
import com.shixincube.app.model.Account;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.ProfileInfoView;
import com.shixincube.app.util.AvatarUtils;
import com.shixincube.app.util.UIUtils;

import cube.contact.model.Self;
import cube.engine.CubeEngine;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 个人信息详情。
 */
public class ProfileInfoPresenter extends BasePresenter<ProfileInfoView> {

    public ProfileInfoPresenter(BaseActivity activity) {
        super(activity);
    }

    public String getAvatarName() {
        return Account.getAvatar(CubeEngine.getInstance().getContactService().getSelf());
    }

    public void loadAccountInfo() {
        Self self = CubeEngine.getInstance().getContactService().getSelf();
        getView().getAvatarImage().setImageResource(AvatarUtils.getAvatarResource(self));
        getView().getNickNameItem().setEndText(self.getPriorityName());
        getView().getCubeIdItem().setEndText(self.getId().toString());
    }

    public void modifyAvatar(String avatarName) {
        Explorer.getInstance().setAccountInfo(AccountHelper.getInstance().getTokenCode(),
                null, avatarName)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(accountInfoResponse -> {
                    Account account = accountInfoResponse.toAccount();
                    // 更新本地数据
                    AccountHelper.getInstance().updateCurrentAccount(account.name, account.avatar);
                    // 更新引擎数据
                    Self self = CubeEngine.getInstance().getContactService().modifySelf(account.name, account.toJSON());

                    activity.runOnUiThread(() -> {
                        getView().getAvatarImage().setImageResource(AvatarUtils.getAvatarResource(self));
                    });
                }, (throwable -> {
                    activity.runOnUiThread(() -> {
                        UIUtils.showToast(UIUtils.getString(R.string.operate_failure));
                    });
                }));
    }
}
