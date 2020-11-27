package cube.common.callback;

import cube.utils.UIHandler;

/**
 * 1个参数，UI线程回调接口
 *
 * @author LiuFeng
 * @data 2020/7/16 11:05
 */
public abstract class UICallback1<T> implements CubeCallback1<T> {

    @Override
    public void onSuccess(T result) {
        UIHandler.run(() -> onSucceed(result));
    }

    @Override
    public void onError(int code, String desc) {
        UIHandler.run(() -> onFailed(code, desc));
    }

    /**
     * UI线程回调成功
     *
     * @param result
     */
    public abstract void onSucceed(T result);

    /**
     * UI线程回调失败
     *
     * @param code
     * @param desc
     */
    public abstract void onFailed(int code, String desc);
}
