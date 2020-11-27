package cube.common.callback;

/**
 * 简化回调接口
 *
 * @author LiuFeng
 * @data 2020/7/16 11:08
 */
public class SimpleCallback1<T> implements CubeCallback1<T> {

    @Override
    public void onSuccess(T result) {}

    @Override
    public void onError(int code, String desc) {}
}
