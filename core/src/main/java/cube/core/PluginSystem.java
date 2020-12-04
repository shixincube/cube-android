package cube.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件系统
 *
 * @author LiuFeng
 * @data 2020/12/3 22:07
 */
public class PluginSystem {
    private static final PluginSystem instance = new PluginSystem();
    private DummyHook dummyHook = new DummyHook();
    private Map<String, Hook> hookMap = new ConcurrentHashMap<>();
    private Map<String, List<Plugin>> pluginMap = new ConcurrentHashMap<>();

    private PluginSystem() {}

    public static PluginSystem getInstance() {
        return instance;
    }

    /**
     * 添加事件钩子。
     *
     * @param hook
     */
    public void addHook(Hook hook) {
        hookMap.put(hook.getName(), hook);
    }

    /**
     * 移除事件钩子。
     *
     * @param hook
     */
    public void removeHook(Hook hook) {
        hookMap.remove(hook.getName());
    }

    /**
     * 移除事件钩子。
     *
     * @param name
     */
    public void removeHook(String name) {
        hookMap.remove(name);
    }

    /**
     * 获取指定事件名的钩子。
     *
     * @param name
     * @return
     */
    public Hook getHook(String name) {
        Hook hook = hookMap.get(name);
        if (hook != null) {
            return hook;
        }

        return dummyHook;
    }

    /**
     * 注册插件。
     *
     * @param name
     * @param plugin
     */
    public void register(String name, Plugin plugin) {
        List<Plugin> plugins = pluginMap.get(name);
        if (plugins == null) {
            plugins = new ArrayList<>();
            pluginMap.put(name, plugins);
        }

        plugins.add(plugin);
    }

    /**
     * 注销插件。
     *
     * @param name
     * @param plugin
     */
    public void deregister(String name, Plugin plugin) {
        List<Plugin> plugins = pluginMap.get(name);
        if (plugins != null) {
            plugins.remove(plugin);
        }
    }

    /**
     * 注销name所有插件。
     *
     * @param name
     */
    public void deregister(String name) {
        pluginMap.remove(name);
    }

    /**
     * 同步方式进行数据处理。
     *
     * @param name
     * @param data
     * @return
     */
    public <T1, T2> T2 syncApply(String name, T1 data) {
        List<Plugin> plugins = pluginMap.get(name);
        if (plugins == null || plugins.isEmpty()) {
            // 没有找到插件直接返回数据
            return (T2) data;
        }

        T2 result = (T2) data;
        for (Plugin plugin : plugins) {
            result = (T2) plugin.onEvent(name, data);
        }

        return result;
    }
}
