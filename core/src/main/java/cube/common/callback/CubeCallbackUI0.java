package cube.common.callback;


import androidx.annotation.NonNull;
import cube.utils.UIHandler;

/**
 * 0个参数，参数接口回调到UI线程
 *
 * @author LiuFeng
 * @data 2020/7/16 11:05
 */
public class CubeCallbackUI0 implements CubeCallback0 {
    CubeCallback0 callback;

    public CubeCallbackUI0(@NonNull CubeCallback0 callback) {
        this.callback = callback;
    }

    @Override
    public void onSuccess() {
        if (callback != null) {
            UIHandler.run(() -> callback.onSuccess());
        }
    }

    @Override
    public void onError(int code, String desc) {
        if (callback != null) {
            UIHandler.run(() -> callback.onError(code, desc));
        }
    }
}
