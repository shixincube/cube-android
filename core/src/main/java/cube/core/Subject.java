package cube.core;

import android.text.TextUtils;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 观察者主题
 *
 * @author LiuFeng
 * @data 2020/9/2 10:21
 */
public abstract class Subject {
    private ConcurrentHashMap<String, Queue<Observer<?>>> observerMap;

    protected Subject() {
        observerMap = new ConcurrentHashMap<>();
    }

    /**
     * 添加指定状态名的观察者。
     *
     * @param event
     * @param observer
     */
    public synchronized <T> void on(String event, Observer<T> observer) {
        if (TextUtils.isEmpty(event) || observer == null) {
            return;
        }

        Queue<Observer<?>> observers = observerMap.get(event);
        if (observers == null) {
            observers = new ConcurrentLinkedQueue<>();
            Queue<Observer<?>> tempObservers = observerMap.putIfAbsent(event, observers);
            if (tempObservers != null) {
                observers = tempObservers;
            }
        }
        observers.add(observer);
        observerMap.put(event, observers);
    }

    /**
     * 移除指定状态名的观察者。
     *
     * @param event
     */
    public synchronized <T> void off(String event) {
        observerMap.remove(event);
    }

    /**
     * 移除指定状态名的观察者。
     *
     * @param event
     * @param observer
     */
    public synchronized <T> void off(String event, Observer<T> observer) {
        if (TextUtils.isEmpty(event) || observer == null) {
            return;
        }

        Queue<Observer<?>> observers = observerMap.get(event);
        if (observers != null) {
            Iterator<?> iterator = observers.iterator();
            while (iterator.hasNext()) {
                T t = (T) iterator.next();
                if (observer.equals(t)) {
                    iterator.remove();
                    break;
                }
            }
        }
    }

    /**
     * 通知观察者有新状态更新。
     *
     * @param data
     * @param <T>
     */
    public synchronized <T> void notify(String event, T data) {
        if (TextUtils.isEmpty(event)) {
            return;
        }

        Queue<Observer<?>> observers = observerMap.get(event);
        if (observers != null) {
            for (Observer observer : observers) {
                observer.update(event, data);
            }
        }
    }
}
