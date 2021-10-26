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

package cube.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 可被观察的主题。
 */
public class Subject {

    private List<Observer> observers;

    private Map<String, List<Observer>> namedObservers;

    public Subject() {
    }

    /**
     * 添加无事件观察者。
     *
     * @param observer 指定观察者对象。
     */
    public void attach(Observer observer) {
        synchronized (this) {
            if (null == this.observers) {
                this.observers = new ArrayList<>();
                this.observers.add(observer);
            }
            else {
                if (!this.observers.contains(observer)) {
                    this.observers.add(observer);
                }
            }
        }
    }

    /**
     * 添加指定事件名观察者。
     *
     * @param name 指定事件名称。
     * @param observer 指定观察者对象。
     */
    public void attachWithName(String name, Observer observer) {
        synchronized (this) {
            if (null == this.namedObservers) {
                this.namedObservers = new HashMap<>();
                List<Observer> list = new ArrayList<>();
                list.add(observer);
                this.namedObservers.put(name, list);
            }
            else {
                List<Observer> list = this.namedObservers.get(name);
                if (null == list) {
                    list = new ArrayList();
                    list.add(observer);
                    this.namedObservers.put(name, list);
                }
                else {
                    if (!list.contains(observer)) {
                        list.add(observer);
                    }
                }
            }
        }
    }

    /**
     * 移除无事件观察者。
     *
     * @param observer 指定观察者对象。
     */
    public void detach(Observer observer) {
        synchronized (this) {
            if (null == this.observers) {
                return;
            }

            this.observers.remove(observer);
        }
    }

    /**
     * 移除指定事件名观察者。
     *
     * @param name 指定事件名称。
     * @param observer 指定观察者对象。
     */
    public void detachWithName(String name, Observer observer) {
        synchronized (this) {
            if (null == this.namedObservers) {
                return;
            }

            List<Observer> list = this.namedObservers.get(name);
            if (null == list) {
                return;
            }

            list.remove(observer);
        }
    }

    /**
     * 通知观察者有新事件更新。
     *
     * @param event 指定新的事件。
     */
    public void notifyObservers(ObservableEvent event) {
        synchronized (this) {
            event.subject = this;

            if (null != this.observers) {
                for (Observer observer : this.observers) {
                    observer.update(event);
                }
            }

            if (null != this.namedObservers) {
                List<Observer> list = this.namedObservers.get(event.name);
                if (null != list) {
                    for (Observer observer : list) {
                        observer.update(event);
                    }
                }
            }
        }
    }
}
