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
import com.shixincube.app.ui.view.ProfileView;
import com.shixincube.app.util.AvatarUtils;
import com.shixincube.app.util.UIUtils;

import cube.contact.model.Self;
import cube.engine.CubeEngine;

/**
 * 个人与应用信息管理。
 */
public class ProfilePresenter extends BasePresenter<ProfileView> {

    public ProfilePresenter(BaseActivity activity) {
        super(activity);
    }

    public void loadAccountInfo() {
        // 获取当前账号数据
        Self self = CubeEngine.getInstance().getContactService().getSelf();
        if (null == self) {
            return;
        }

        getView().getAvatarImage().setImageResource(AvatarUtils.getAvatarResource(self));
        getView().getNickNameText().setText(self.getPriorityName());
        getView().getCubeIdText().setText(UIUtils.getString(R.string.cube_id_colon, self.getId().toString()));
    }
}
