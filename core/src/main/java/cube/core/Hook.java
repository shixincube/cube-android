package cube.core;

/**
 * 事件钩子
 *
 * @author LiuFeng
 * @data 2020/12/3 22:05
 */
public class Hook {
    /**
     * 触发钩子的事件名。
     */
    private String name;

    public Hook(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * 触发钩子，从系统里找到对应的插件进行数据处理，并返回处理后的数据。
     *
     * @param data 指定触发事件时的数据。
     * @return 返回处理后的数据。
     */
    public <T1, T2> T2 apply(T1 data) {
        return PluginSystem.getInstance().syncApply(name, data);
    }
}
