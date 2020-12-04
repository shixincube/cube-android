package cube.core;

/**
 * 插件
 *
 * @author LiuFeng
 * @data 2020/12/3 22:15
 */
public interface Plugin<T1, T2> {

    /**
     * 当指定的钩子事件发生时回调该函数。
     *
     * @param name 事件名称。
     * @param data 事件发生时的数据。
     * @return 返回处理后的数据。
     */
    T2 onEvent(String name, T1 data);
}
