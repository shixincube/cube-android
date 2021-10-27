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
import java.util.HashMap;
import java.util.List;

/**
 * 插件系统。
 */
public class PluginSystem {

    private HashMap<String, Hook> hooks;

    private HashMap<String, List<Plugin>> plugins;

    private DummyHook dummyHook;

    public PluginSystem() {
        this.hooks = new HashMap<>();
        this.plugins = new HashMap<>();
        this.dummyHook = new DummyHook();
    }

    /**
     * 添加事件钩子。
     *
     * @param hook 指定钩子实例。
     */
    public void addHook(Hook<?> hook) {
        hook.system = this;
        this.hooks.put(hook.name, hook);
    }

    /**
     * 移除事件钩子。
     *
     * @param hook 指定钩子实例。
     */
    public void removeHook(Hook<?> hook) {
        this.hooks.remove(hook.name);
        hook.system = null;
    }

    /**
     * 获取指定事件名的钩子。
     *
     * @param name 指定事件名。
     * @return 返回指定事件名的钩子。
     */
    public Hook<?> getHook(String name) {
        Hook<?> hook = this.hooks.get(name);
        if (null != hook) {
            return hook;
        }

        return this.dummyHook;
    }

    /**
     * 清空钩子。
     */
    public void clearHooks() {
        this.hooks.clear();
    }

    /**
     * 注册插件。
     *
     * @param name 钩子事件名。
     * @param plugin 插件实例。
     */
    public void registerPlugin(String name, Plugin<?> plugin) {
        List<Plugin> list = this.plugins.get(name);
        if (null != list) {
            if (!list.contains(plugin)) {
                list.add(plugin);
            }
        }
        else {
            list = new ArrayList<>();
            list.add(plugin);
            this.plugins.put(name, list);
        }
    }

    /**
     * 注销插件。
     *
     * @param name 钩子事件名。
     * @param plugin 插件实例。
     */
    public void deregisterPlugin(String name, Plugin<?> plugin) {
        List<Plugin> list = this.plugins.get(name);
        if (null != list) {
            list.remove(plugin);
        }
    }

    /**
     * 清空插件。
     */
    public void clearPlugins() {
        this.plugins.clear();
    }

    /**
     * 同步方式进行数据处理。
     *
     * @param name 钩子名称。
     * @param data 待处理数据。
     * @return 返回处理后的数据。
     */
    protected <T> T syncApply(String name, T data) {
        List<Plugin> list = this.plugins.get(name);
        if (null != list) {
            T result = data;
            for (Plugin<T> plugin : list) {
                result = plugin.onEvent(name, result);
            }
            return result;
        }
        else {
            return data;
        }
    }

    /**
     * 哑元钩子。
     */
    private class DummyHook extends Hook<Object> {

        public DummyHook() {
            super("CubeDummyHook");
        }

        @Override
        public Object apply(Object data) {
            return data;
        }
    }
}
