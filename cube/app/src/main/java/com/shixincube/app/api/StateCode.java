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

package com.shixincube.app.api;

/**
 * 状态码。
 */
public final class StateCode {

    /**
     * 成功。
     */
    public final static int Success = 0;

    /**
     * 不被允许的行为。
     */
    public final static int NotAllowed = 1;

    /**
     * 找不到用户。
     */
    public final static int NotFindAccount = 5;

    /**
     * 无效的令牌。
     */
    public final static int InvalidToken = 6;

    /**
     * 找不到令牌。
     */
    public final static int NotFindToken = 7;

    /**
     * 无效账号。
     */
    public final static int InvalidAccount = 8;

    /**
     * 数据错误。
     */
    public final static int DataError = 9;

    /**
     * 其他状态。
     */
    public final static int Other = 99;

    private StateCode() {
    }
}
