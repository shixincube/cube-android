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

import android.view.View;
import android.widget.LinearLayout;

import com.shixincube.app.R;

/**
 * 文件标签控制器。
 */
public class FilesTabController implements View.OnClickListener {

    public final static int TAB_ALL_FILE = 1;
    public final static int TAB_IMAGE_FILE = 2;
    public final static int TAB_DOC_FILE = 3;
    public final static int TAB_VIDEO_FILE = 4;
    public final static int TAB_AUDIO_FILE = 5;

    private LinearLayout rootLayout;

    private LinearLayout activeTab;

    public FilesTabController(LinearLayout rootLayout) {
        this.rootLayout = rootLayout;
        this.rootLayout.findViewById(R.id.llAllFiles).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (null != this.activeTab) {
            if (id == this.activeTab.getId()) {
                return;
            }
        }
    }
}
