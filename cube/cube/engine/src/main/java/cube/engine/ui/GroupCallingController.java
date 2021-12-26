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

package cube.engine.ui;

import android.media.AudioManager;
import android.util.Size;
import android.view.ViewGroup;

import java.util.List;

import cube.contact.model.Contact;
import cube.contact.model.Group;
import cube.engine.service.FloatingVideoWindowService;
import cube.multipointcomm.util.MediaConstraint;

/**
 * 群组通话控制器。
 */
public class GroupCallingController implements Controller {

    private FloatingVideoWindowService service;
    private AudioManager audioManager;

    private ViewGroup mainLayout;

    private MediaConstraint mediaConstraint;

    private Group group;
    private List<Contact> members;

    public GroupCallingController(FloatingVideoWindowService service, AudioManager audioManager, ViewGroup mainLayout) {
        this.service = service;
        this.audioManager = audioManager;
        this.mainLayout = mainLayout;
    }

    public void config(MediaConstraint mediaConstraint) {
        this.mediaConstraint = mediaConstraint;
    }

    public void set(Group group, List<Contact> contactList) {
        this.group = group;
        this.members = contactList;
    }

    @Override
    public Size reset() {
        return null;
    }

    @Override
    public ViewGroup getMainLayout() {
        return this.mainLayout;
    }

    @Override
    public boolean isShown() {
        return this.mainLayout.isShown();
    }
}
