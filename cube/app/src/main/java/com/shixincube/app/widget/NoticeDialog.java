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

package com.shixincube.app.widget;

import android.app.Dialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.shixincube.app.R;

/**
 * 通知对话框。
 */
public class NoticeDialog extends Dialog {

    private TextView titleView;
    private TextView contentView;

    private Button defaultButton;

    public NoticeDialog(Context context) {
        super(context, R.style.FullDialog);
        this.setContentView(R.layout.dialog_notice);
        this.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        this.titleView = findViewById(R.id.tvTitle);
        this.contentView = findViewById(R.id.tvContent);
        this.defaultButton = findViewById(R.id.btnISee);

        this.defaultButton.setOnClickListener((view) -> {
            dismiss();
        });
    }

    public void show(String title, String content) {
        this.titleView.setText(title);
        this.contentView.setText(content);

        this.show();
    }
}
