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

package cube.core;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import cube.core.model.Entity;

/**
 * 实体管理器，用于维护实体的生命周期。
 */
public final class EntityInspector extends TimerTask {

    private Timer timer;

    private List<Map<Long, Entity>> depositedMapArray;

    public EntityInspector() {
        this.depositedMapArray = new Vector<>();
    }

    public void start() {
        if (null == this.timer) {
            this.timer = new Timer();
            this.timer.schedule(this, 30L * 1000L, 30L * 1000L);
        }
    }

    public void stop() {
        if (null != this.timer) {
            this.timer.cancel();
            this.timer = null;
        }
    }

    public void depositMap(Map<Long, Entity> map) {

    }

    public void withdrawMap(Map<Long, Entity> map) {

    }

    @Override
    public void run() {

    }
}
