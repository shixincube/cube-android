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
import android.widget.TextView;

import com.shixincube.app.R;
import com.shixincube.app.util.UIUtils;

/**
 * 文件标签控制器。
 */
public class FilesTabController implements View.OnClickListener {

    public final static int TAB_ALL_FILES = 1;
    public final static int TAB_IMAGE_FILES = 2;
    public final static int TAB_DOC_FILES = 3;
    public final static int TAB_VIDEO_FILES = 4;
    public final static int TAB_AUDIO_FILES = 5;
    public final static int TAB_TRASH_FILES = 6;
    public final static int TAB_TRANSMITTING_FILES = 7;

    private LinearLayout rootLayout;

    private int activeTab;
    private int activeTabId;

    private TabChangedListener tabChangedListener;

    public FilesTabController() {
    }

    public void bind(LinearLayout rootLayout) {
        this.rootLayout = rootLayout;
        this.activeTab = TAB_ALL_FILES;
        this.activeTabId = R.id.llAllFiles;

        this.rootLayout.findViewById(R.id.llAllFiles).setOnClickListener(this);
        this.rootLayout.findViewById(R.id.llImageFiles).setOnClickListener(this);
        this.rootLayout.findViewById(R.id.llDocFiles).setOnClickListener(this);
        this.rootLayout.findViewById(R.id.llVideoFiles).setOnClickListener(this);
        this.rootLayout.findViewById(R.id.llAudioFiles).setOnClickListener(this);
        this.rootLayout.findViewById(R.id.llTrash).setOnClickListener(this);
        this.rootLayout.findViewById(R.id.llTransmitting).setOnClickListener(this);
    }

    public void setTabChangedListener(TabChangedListener listener) {
        this.tabChangedListener = listener;
    }

    public int getActiveTab() {
        return this.activeTab;
    }

    public void setTransmittingNum(int num) {
        TextView badge = this.rootLayout.findViewById(R.id.llTransmitting)
                .findViewById(R.id.tvBadgeTransmitting);
        if (num > 0) {
            badge.setVisibility(View.VISIBLE);
            badge.setText(Integer.toString(num));
        }
        else {
            badge.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == this.activeTabId) {
            return;
        }

        LinearLayout current = this.rootLayout.findViewById(this.activeTabId);
        current.setBackgroundResource(R.drawable.shape_file_tab_normal);
        ((TextView) current.findViewWithTag("98"))
                .setTextColor(UIUtils.getColorByAttrId(R.attr.colorButtonSecondaryText));

        this.activeTabId = id;

        switch (id) {
            case R.id.llAllFiles:
                this.activeTab = TAB_ALL_FILES;
                break;
            case R.id.llImageFiles:
                this.activeTab = TAB_IMAGE_FILES;
                break;
            case R.id.llDocFiles:
                this.activeTab = TAB_DOC_FILES;
                break;
            case R.id.llVideoFiles:
                this.activeTab = TAB_VIDEO_FILES;
                break;
            case R.id.llAudioFiles:
                this.activeTab = TAB_AUDIO_FILES;
                break;
            case R.id.llTrash:
                this.activeTab = TAB_TRASH_FILES;
                break;
            case R.id.llTransmitting:
                this.activeTab = TAB_TRANSMITTING_FILES;
                break;
            default:
                break;
        }

        view.setBackgroundResource(R.drawable.shape_file_tab_pressed);
        ((TextView) view.findViewWithTag("98"))
                .setTextColor(UIUtils.getColorByAttrId(R.attr.colorButtonPrimaryText));

        this.tabChangedListener.onTabChanged(this.activeTab);
    }

    /**
     * 标签变更监听器。
     */
    public interface TabChangedListener {
        void onTabChanged(int tab);
    }
}
