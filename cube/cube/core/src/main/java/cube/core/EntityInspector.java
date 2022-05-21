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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import cube.core.model.Entity;
import cube.core.model.TimeSortable;
import cube.util.LogUtils;

/**
 * 实体内存生命周期检查器。
 * 用于维护实体在内存里存储的生命周期。
 * 当实体过期或者占用内存空间超过阀值时释放实体引用。
 */
public final class EntityInspector implements Runnable {

    /**
     * 最大内存阀值。
     */
    private long maxMemoryThreshold = 8 * 1024 * 1024;

    private Timer timer;

    private long period = 5 * 60 * 1000;

    private long lastTime = 0;

    private List<Map<? extends Object, ? extends Entity>> depositedMapArray;

    private List<List<? extends Entity>> depositedListArray;

    public EntityInspector() {
        this.depositedMapArray = new Vector<>();
        this.depositedListArray = new Vector<>();
    }

    /**
     * 启动。
     */
    public void start() {
        synchronized (this) {
            if (null != this.timer) {
                this.timer.cancel();
                this.timer.purge();
            }

            this.timer = new Timer();
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    EntityInspector.this.run();
                }
            }, this.period, this.period);
        }
    }

    /**
     * 停止。
     */
    public void stop() {
        synchronized (this) {
            if (null != this.timer) {
                this.timer.cancel();
                this.timer.purge();
                this.timer = null;
            }
        }

        this.depositedMapArray.clear();
        this.depositedListArray.clear();
    }

    /**
     * 检测定时器。
     */
    public void check() {
        if (System.currentTimeMillis() - this.lastTime > this.period) {
            this.stop();
            this.start();
        }
    }

    /**
     * 存入指定映射进行生命周期管理。
     *
     * @param map
     */
    public void depositMap(Map<? extends Object, ? extends Entity> map) {
        if (!this.depositedMapArray.contains(map)) {
            this.depositedMapArray.add(map);
        }
    }

    /**
     * 解除指定映射的生命周期管理。
     *
     * @param map
     */
    public void withdrawMap(Map<? extends Object, ? extends Entity> map) {
        this.depositedMapArray.remove(map);
    }

    /**
     * 存入指定列表进行生命周期管理。
     *
     * @param list
     */
    public void depositList(List<? extends Entity> list) {
        if (!this.depositedListArray.contains(list)) {
            this.depositedListArray.add(list);
        }
    }

    /**
     * 解除指定列表的生命周期管理。
     *
     * @param list
     */
    public void withdrawList(List<? extends Entity> list) {
        this.depositedListArray.remove(list);
    }

    private void removeEntity(Entity current) {
        for (Map<? extends Object, ? extends Entity> map : this.depositedMapArray) {
            Iterator<? extends Map.Entry<? extends Object, ? extends Entity>> iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<? extends Object, ? extends Entity> e = iter.next();
                Entity entity = e.getValue();
                if (entity.equals(current)) {
                    iter.remove();
                    return;
                }
            }
        }

        for (List<? extends Entity> list : this.depositedListArray) {
            Iterator<? extends Entity> iter = list.iterator();
            while (iter.hasNext()) {
                Entity entity = iter.next();
                if (entity.equals(current)) {
                    iter.remove();
                    return;
                }
            }
        }
    }

    @Override
    public void run() {
        this.lastTime = System.currentTimeMillis();

        long size = 0;

        ArrayList<Entity> entityList = new ArrayList<>();

        for (Map<? extends Object, ? extends Entity> map : this.depositedMapArray) {
            Iterator<? extends Map.Entry<? extends Object, ? extends Entity>> iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<? extends Object, ? extends Entity> e = iter.next();
                Entity entity = e.getValue();
                size += entity.getMemorySize();
                entityList.add(entity);
            }
        }

        for (List<? extends Entity> list : this.depositedListArray) {
            Iterator<? extends Entity> iter = list.iterator();
            while (iter.hasNext()) {
                Entity entity = iter.next();
                size += entity.getMemorySize();
                entityList.add(entity);
            }
        }

        if (size < this.maxMemoryThreshold) {
            // 内存未超过阀值
            if (LogUtils.isDebugLevel()) {
                LogUtils.d("EntityInspector", "Memory usage: " + size + "/" + this.maxMemoryThreshold);
            }
            entityList.clear();
            return;
        }

        if (LogUtils.isDebugLevel()) {
            LogUtils.d("EntityInspector", "Memory size: " + size + "/" + this.maxMemoryThreshold);
        }

        // 按照超时时间排序
        Collections.sort(entityList, new Comparator<TimeSortable>() {
            @Override
            public int compare(TimeSortable entity1, TimeSortable entity2) {
                return (int) (entity1.getSortableTime() - entity2.getSortableTime());
            }
        });

        // 计算需要删除哪些实体
        int position = 0;
        long delta = 0;
        while (position < entityList.size()) {
            Entity entity = entityList.get(position);

            if (entity.getSortableTime() > this.lastTime) {
                // 数据没有超期
                --position;
                break;
            }

            delta += entity.getMemorySize();
            if (size - delta < this.maxMemoryThreshold) {
                break;
            }
            ++position;
        }

        // 从时间戳最小的实体删除到 position 位置
        for (int i = 0; i <= position; ++i) {
            Entity entity = entityList.get(i);
            this.removeEntity(entity);
        }

        entityList.clear();

        if (LogUtils.isDebugLevel()) {
            LogUtils.d("EntityInspector", "Clear " + (position + 1) + " entities");
        }
    }
}
