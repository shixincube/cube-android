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

package cube.contact;

import org.json.JSONObject;

import cube.contact.model.Contact;

/**
 * 联系人数据提供者。
 */
public interface ContactDataProvider {

    /**
     * 当联系人服务模块需要读取联系人上下文数据时，调用该方法。
     * 一般的，联系人的 {@code context} 可用于存储应用程序账号的相关数据。
     * 因此建议实现该方法，这样任何需要使用 {@link Contact} 数据的地方都可以使用这些账号数据。
     *
     * @param contact 需要上下文的联系人实例。
     * @return 返回有效的联系人数据，如果返回 {@code null} 值，则不设置上下文。
     */
    JSONObject needContactContext(Contact contact);
}
