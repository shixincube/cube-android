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

package com.shixincube.app.api;

import java.util.concurrent.TimeUnit;

import cube.util.LogUtils;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Function;

/**
 * 重试操作。
 */
public class RetryWithDelay implements Function<Observable<? extends Throwable>, Observable<?>> {

    private final long retryDelayMillis;

    private final int maxRetries;

    private int retryCount;

    public RetryWithDelay(long retryDelayMillis, int maxRetries) {
        this.retryDelayMillis = retryDelayMillis;
        this.maxRetries = maxRetries;
        this.retryCount = 0;
    }

    @Override
    public Observable<?> apply(Observable<? extends Throwable> attempts) throws Throwable {
        return attempts.flatMap((Throwable throwable) -> {
            if (++retryCount <= maxRetries) {
                LogUtils.i("RetryWithDelay", "Get error, it will try after "
                        + retryDelayMillis + " millisecond, retry count " + retryCount);
                return Observable.timer(retryDelayMillis, TimeUnit.MILLISECONDS);
            }

            return Observable.error(throwable);
        });
    }
}
