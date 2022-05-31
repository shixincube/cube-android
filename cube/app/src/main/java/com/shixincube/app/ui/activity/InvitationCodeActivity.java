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

import android.content.Intent;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.InvitationCodeInputView;

import butterknife.BindView;

/**
 * 邀请码输入界面。
 */
public class InvitationCodeActivity extends BaseActivity {

    public final static int RESULT = 1001;

    public final static String EXTRA_CODE = "code";

    @BindView(R.id.iciCode)
    InvitationCodeInputView codeInputView;

    public InvitationCodeActivity() {
        super();
    }

    @Override
    public void initView() {
        setToolbarTitle(UIUtils.getString(R.string.ferry_box_invi_code));
    }

    @Override
    public void initListener() {
        this.codeInputView.setOnInputListener(new InvitationCodeInputView.OnInputListener() {
            @Override
            public void onInput(InvitationCodeInputView view) {
                // Nothing
            }

            @Override
            public void onFinish(InvitationCodeInputView view, String code) {
                respond(code);
            }
        });

        this.codeInputView.showSoftInput();
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_invitation_code;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void respond(String code) {
        Intent data = new Intent();
        data.putExtra(EXTRA_CODE, code);
        setResult(RESULT, data);
        finish();
    }
}
