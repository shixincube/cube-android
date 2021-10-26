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

package com.shixincube.app.ui.fragment;

/**
 * 主界面实例化工厂。
 */
public final class FragmentFactory {

    private final static FragmentFactory instance = new FragmentFactory();

    private ConversationFragment conversationFragment;
    private FilesFragment filesFragment;
    private ContactsFragment contactsFragment;
    private ProfileFragment profileFragment;

    private FragmentFactory() {
    }

    public final static FragmentFactory getInstance() {
        return FragmentFactory.instance;
    }

    public ConversationFragment getConversationFragment() {
        if (null == this.conversationFragment) {
            this.conversationFragment = new ConversationFragment();
        }
        return this.conversationFragment;
    }

    public FilesFragment getFilesFragment() {
        if (null == this.filesFragment) {
            this.filesFragment = new FilesFragment();
        }
        return this.filesFragment;
    }

    public ContactsFragment getContactsFragment() {
        if (null == this.contactsFragment) {
            this.contactsFragment = new ContactsFragment();
        }
        return this.contactsFragment;
    }

    public ProfileFragment getProfileFragment() {
        if (null == this.profileFragment) {
            this.profileFragment = new ProfileFragment();
        }
        return this.profileFragment;
    }
}
