package cube.common.callback;

import cube.utils.UIHandler;

/**
 * 0个参数，UI线程回调接口
 *
 * @author LiuFeng
 * @data 2020/7/16 11:05
 */
public abstract class UICallback0 implements CubeCallback0 {

    @Override
    public void onSuccess() {
        UIHandler.run(this::onSucceed);
    }

    @Override
    public void onError(int code, String desc) {
        UIHandler.run(() -> onFailed(code, desc));
    }

    /**
     * UI线程回调成功
     */
    public abstract void onSucceed();

    /**
     * UI线程回调失败
     *
     * @param code
     * @param desc
     */
    public abstract void onFailed(int code, String desc);
}
