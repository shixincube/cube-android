package cube.core;

import java.io.Serializable;

/**
 * 观察者
 *
 * @author LiuFeng
 * @data 2020/9/2 10:29
 */
public interface Observer<T> extends Serializable {

    /**
     * 更新状态
     *
     * @param event
     * @param data
     */
    void update(String event, T data);
}
