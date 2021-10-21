/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Shixin Cube Team.
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

import com.shixincube.app.R;
import com.shixincube.app.ui.view.MainView;
import com.shixincube.app.util.UIUtils;

/**
 * 主标签栏。
 */
public class MainTabBar {

    private MainView view;

    public MainTabBar(MainView view) {
        this.view = view;

        this.reset();
    }

    public void toggleToConversation() {
        this.reset();

        this.view.getConversationPressedIcon().setVisibility(View.VISIBLE);
        this.view.getConversationPressedTitle().setVisibility(View.VISIBLE);
        this.view.getConversationNormalIcon().setVisibility(View.INVISIBLE);
        this.view.getConversationNormalTitle().setVisibility(View.INVISIBLE);

        this.view.setToolbarTitle(UIUtils.getString(R.string.conversation));
    }

    public void toggleToFiles() {
        this.reset();

        this.view.getFilesPressedIcon().setVisibility(View.VISIBLE);
        this.view.getFilesPressedTitle().setVisibility(View.VISIBLE);
        this.view.getFilesNormalIcon().setVisibility(View.INVISIBLE);
        this.view.getFilesNormalTitle().setVisibility(View.INVISIBLE);

        this.view.setToolbarTitle(UIUtils.getString(R.string.files));
    }

    public void toggleToContacts() {
        this.reset();

        this.view.getContactsPressedIcon().setVisibility(View.VISIBLE);
        this.view.getContactsPressedTitle().setVisibility(View.VISIBLE);
        this.view.getContactsNormalIcon().setVisibility(View.INVISIBLE);
        this.view.getContactsNormalTitle().setVisibility(View.INVISIBLE);

        this.view.setToolbarTitle(UIUtils.getString(R.string.contacts));
    }

    public void toggleToProfile() {
        this.reset();

        this.view.getProfilePressedIcon().setVisibility(View.VISIBLE);
        this.view.getProfilePressedTitle().setVisibility(View.VISIBLE);
        this.view.getProfileNormalIcon().setVisibility(View.INVISIBLE);
        this.view.getProfileNormalTitle().setVisibility(View.INVISIBLE);

        this.view.setToolbarTitle(UIUtils.getString(R.string.profile));
    }

    private void reset() {
        this.view.getConversationNormalIcon().setVisibility(View.VISIBLE);
        this.view.getConversationNormalTitle().setVisibility(View.VISIBLE);
        this.view.getConversationPressedIcon().setVisibility(View.INVISIBLE);
        this.view.getConversationPressedTitle().setVisibility(View.INVISIBLE);

        this.view.getFilesNormalIcon().setVisibility(View.VISIBLE);
        this.view.getFilesNormalTitle().setVisibility(View.VISIBLE);
        this.view.getFilesPressedIcon().setVisibility(View.INVISIBLE);
        this.view.getFilesPressedTitle().setVisibility(View.INVISIBLE);

        this.view.getContactsNormalIcon().setVisibility(View.VISIBLE);
        this.view.getContactsNormalTitle().setVisibility(View.VISIBLE);
        this.view.getContactsPressedIcon().setVisibility(View.INVISIBLE);
        this.view.getContactsPressedTitle().setVisibility(View.INVISIBLE);

        this.view.getProfileNormalIcon().setVisibility(View.VISIBLE);
        this.view.getProfileNormalTitle().setVisibility(View.VISIBLE);
        this.view.getProfilePressedIcon().setVisibility(View.INVISIBLE);
        this.view.getProfilePressedTitle().setVisibility(View.INVISIBLE);
    }
}
