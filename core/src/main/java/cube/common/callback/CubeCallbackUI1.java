package cube.common.callback;


import androidx.annotation.NonNull;
import cube.utils.UIHandler;

/**
 * 1个参数，参数接口回调到UI线程
 *
 * @author LiuFeng
 * @data 2020/7/16 11:05
 */
public class CubeCallbackUI1<T> implements CubeCallback1<T> {
    CubeCallback1<T> callback;

    public CubeCallbackUI1(@NonNull CubeCallback1<T> callback) {
        this.callback = callback;
    }

    @Override
    public void onSuccess(T result) {
        if (callback != null) {
            UIHandler.run(() -> callback.onSuccess(result));
        }
    }

    @Override
    public void onError(int code, String desc) {
        if (callback != null) {
            UIHandler.run(() -> callback.onError(code, desc));
        }
    }
}
