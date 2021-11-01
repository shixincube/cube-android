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

package cube.core;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import cube.core.model.Entity;
import cube.util.LogUtils;

/**
 * 实体管理器，用于维护实体的生命周期。
 */
public final class EntityInspector extends TimerTask {

    private Timer timer;

    private List<Map<Long, ? extends Entity>> depositedMapArray;

    private List<List<? extends Entity>> depositedListArray;

//    private Map<Long, Long> lifespanMap;

    private long lifespan;

    public EntityInspector() {
        this.depositedMapArray = new Vector<>();
        this.depositedListArray = new Vector<>();
//        this.lifespanMap = new ConcurrentHashMap<>();
        this.lifespan = 5L * 60L * 1000L;
    }

    /**
     * 启动。
     */
    public void start() {
        if (null == this.timer) {
            this.timer = new Timer();
            this.timer.schedule(this, 30L * 1000L, 30L * 1000L);
        }
    }

    /**
     * 停止。
     */
    public void stop() {
        if (null != this.timer) {
            this.timer.cancel();
            this.timer = null;
        }
    }

    /**
     * 存入指定映射进行生命周期管理。
     *
     * @param map
     */
    public void depositMap(Map<Long, ? extends Entity> map) {
        if (!this.depositedMapArray.contains(map)) {
            this.depositedMapArray.add(map);
        }
    }

    /**
     * 解除指定映射的生命周期管理。
     *
     * @param map
     */
    public void withdrawMap(Map<Long, ? extends Entity> map) {
        this.depositedMapArray.remove(map);
    }

    public void depositList(List<? extends Entity> list) {

    }

    public void withdrawList(List<? extends Entity> list) {

    }

    @Override
    public void run() {
        int count = 0;

        long now = System.currentTimeMillis();

        for (Map<Long, ? extends Entity> map : this.depositedMapArray) {
            Iterator<? extends Map.Entry<Long, ? extends Entity>> iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Long, ? extends Entity> e = iter.next();
                Entity entity = e.getValue();
                if (now - entity.entityCreation > this.lifespan) {
                    iter.remove();
                    ++count;
                }
            }
        }

        for (List<? extends Entity> list : this.depositedListArray) {
            Iterator<? extends Entity> iter = list.iterator();
            while (iter.hasNext()) {
                Entity entity = iter.next();
                if (now - entity.entityCreation > this.lifespan) {
                    iter.remove();
                    ++count;
                }
            }
        }

        LogUtils.d("EntityInspector", "Clear count: " + count);
    }

}
