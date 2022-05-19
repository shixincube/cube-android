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

import androidx.appcompat.app.AlertDialog;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.FerryView;
import com.shixincube.app.util.UIUtils;

import cube.engine.util.CodeUtils;
import cube.util.LogUtils;

/**
 * Ferry Presenter
 */
public class FerryPresenter extends BasePresenter<FerryView> {

    private final static String TAG = FerryPresenter.class.getSimpleName();

    public FerryPresenter(BaseActivity activity) {
        super(activity);
    }

    public void processQRCodeResult(String string) {
        LogUtils.d(TAG, "QR code: " + string);

        if (string.length() < 16) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(UIUtils.getString(R.string.prompt));
            builder.setMessage(UIUtils.getString(R.string.unrecognized_code));
            builder.setNegativeButton(UIUtils.getString(R.string.sure), null);
            builder.show();
            return;
        }

        String protocol = CodeUtils.extractProtocol(string);
        if (!protocol.equalsIgnoreCase("cube")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(UIUtils.getString(R.string.prompt));
            builder.setMessage(UIUtils.getString(R.string.unrecognized_code));
            builder.setNegativeButton(UIUtils.getString(R.string.sure), null);
            builder.show();
            return;
        }

        String[] data = CodeUtils.extractResourceSegments(string);
        if (null == data || data.length != 2) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(UIUtils.getString(R.string.prompt));
            builder.setMessage(UIUtils.getString(R.string.unsupported_qr_format));
            builder.setNegativeButton(UIUtils.getString(R.string.sure), null);
            builder.show();
            return;
        }

        
    }
}
