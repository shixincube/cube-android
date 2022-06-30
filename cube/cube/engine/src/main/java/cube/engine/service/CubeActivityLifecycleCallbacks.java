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

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

public class CubeActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

    private AtomicInteger count;

    public CubeActivityLifecycleCallbacks() {
        this.count = new AtomicInteger(0);
    }

    public boolean isForeground() {
        return this.count.get() > 0;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
        // Nothing
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        this.count.incrementAndGet();
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        // Nothing
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        // Nothing
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        this.count.decrementAndGet();
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
        // Nothing
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        // Nothing
    }
}
