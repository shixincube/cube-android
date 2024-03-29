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

package cube.engine.service;

import android.os.Binder;

import cube.engine.handler.ContactDataHandler;

/**
 * 悬浮窗 Binder 。
 */
public class FloatingVideoWindowBinder extends Binder {

    private FloatingVideoWindowService service;

    private ContactDataHandler contactDataHandler;

    private FloatingVideoWindowListener listener;

    public FloatingVideoWindowBinder(FloatingVideoWindowService service) {
        this.service = service;
    }

    public FloatingVideoWindowService getService() {
        return this.service;
    }

    public void setContactDataHandler(ContactDataHandler contactDataHandler) {
        this.contactDataHandler = contactDataHandler;
    }

    public ContactDataHandler getContactDataHandler() {
        return this.contactDataHandler;
    }

    public void setListener(FloatingVideoWindowListener listener) {
        this.listener = listener;
    }

    public FloatingVideoWindowListener getListener() {
        return this.listener;
    }
}
