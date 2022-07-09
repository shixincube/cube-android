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
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.shixincube.app.R;
import com.shixincube.app.ui.activity.MainActivity;

/**
 * 主底部菜单。
 */
public class MainBottomMenu implements View.OnClickListener {

    private MainActivity activity;

    private LinearLayout shareToContact;
    private LinearLayout shareToHyperlink;
    private LinearLayout shareToWeChat;
    private LinearLayout shareToQQ;
    private LinearLayout shareToOther;

    private OnClickListener clickListener;

    public MainBottomMenu(MainActivity activity, ViewGroup parentView) {
        this.activity = activity;
        LinearLayout sharingMenu = parentView.findViewById(R.id.llSharingMenu);
        this.shareToContact = sharingMenu.findViewById(R.id.llShareToContact);
        this.shareToHyperlink = sharingMenu.findViewById(R.id.llShareToHyperlink);
        this.shareToWeChat = sharingMenu.findViewById(R.id.llShareToWeChat);
        this.shareToQQ = sharingMenu.findViewById(R.id.llShareToQQ);
        this.shareToOther = sharingMenu.findViewById(R.id.llShareToOther);

        this.shareToContact.setOnClickListener(this);
        this.shareToHyperlink.setOnClickListener(this);
        this.shareToWeChat.setOnClickListener(this);
        this.shareToQQ.setOnClickListener(this);
        this.shareToOther.setOnClickListener(this);
    }

    public void setOnClickListener(OnClickListener listener) {
        this.clickListener = listener;
    }

    @Override
    public void onClick(View view) {
        if (null != clickListener) {
            clickListener.onItemClick(view.getId());
        }

        activity.hideBottomMenu();
    }


    /**
     * Click 事件监听器。
     */
    public interface OnClickListener {

        void onItemClick(int resourceId);
    }
}
