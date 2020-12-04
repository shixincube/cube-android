package cube.core;

/**
 * 哑元钩子
 * 备注：空对象模式，避免从插件系统取出null值
 *
 * @author LiuFeng
 * @data 2020/12/3 22:26
 */
public class DummyHook extends Hook {

    public DummyHook() {
        super("DummyHook");
    }
}
