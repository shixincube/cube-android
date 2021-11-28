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
import com.shixincube.app.ui.activity.ContactDetailsActivity;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.ContactDetailsView;
import com.shixincube.app.util.UIUtils;

import cube.contact.handler.DefaultContactZoneHandler;
import cube.contact.model.Contact;
import cube.contact.model.ContactZone;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;

/**
 * 联系人详情。
 */
public class ContactDetailsPresenter extends BasePresenter<ContactDetailsView> {

    private Contact contact;

    public ContactDetailsPresenter(BaseActivity activity, Contact contact) {
        super(activity);
        this.contact = contact;
    }

    public void deleteContact() {
        activity.showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

        // 获取默认联系人分区，将联系人移除
        CubeEngine.getInstance().getContactService()
                .getDefaultContactZone()
                .removeParticipant(contact, new DefaultContactZoneHandler(true) {
                    @Override
                    public void handleContactZone(ContactZone contactZone) {
                        activity.hideWaitingDialog();

                        activity.setResult(ContactDetailsActivity.RESULT_REMOVE);
                        activity.finish();
                    }
                }, new DefaultFailureHandler(true) {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {
                        activity.hideWaitingDialog();
                        UIUtils.showToast(UIUtils.getString(R.string.operate_failure));
                    }
                });
    }
}
