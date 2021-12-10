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

import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.UIUtils;

import butterknife.BindView;
import cube.contact.handler.DefaultContactAppendixHandler;
import cube.contact.model.Contact;
import cube.contact.model.ContactAppendix;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;

/**
 * 备注和标签界面。
 */
public class RemarkTagActivity extends BaseActivity {

    @BindView(R.id.etRemark)
    EditText remarkEdit;

    @BindView(R.id.llTag)
    LinearLayout tagLayout;

    private String origRemark;

    private Contact contact;

    public RemarkTagActivity() {
        super();
    }

    @Override
    public void initView() {
        this.toolbarFuncButton.setText(UIUtils.getString(R.string.complete));
        this.toolbarFuncButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void initListener() {
        this.toolbarFuncButton.setOnClickListener((view) -> {
            String remark = remarkEdit.getText().toString().trim();
            if (!origRemark.equals(remark)) {
                contact.getAppendix().modifyRemarkName(remark, new DefaultContactAppendixHandler(true) {
                    @Override
                    public void handleAppendix(Contact contact, ContactAppendix appendix) {
                        setResult(RESULT_OK);
                        finish();
                    }
                }, new DefaultFailureHandler(true) {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
            }
            else {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    public void initData() {
        Long contactId = getIntent().getLongExtra("contactId", 0);
        this.contact = CubeEngine.getInstance().getContactService().getContact(contactId);

        if (this.contact.getAppendix().hasRemarkName()) {
            this.origRemark = this.contact.getAppendix().getRemarkName();
            this.remarkEdit.setText(this.origRemark);
        }
        else {
            this.origRemark = "";
        }
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_remark_tag_setting;
    }
}
