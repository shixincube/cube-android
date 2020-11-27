package cube.common.callback;

/**
 * 简化回调接口
 *
 * @author LiuFeng
 * @data 2020/7/16 11:08
 */
public class SimpleCallback2<T1, T2> implements CubeCallback2<T1, T2> {

    @Override
    public void onSuccess(T1 result1, T2 result2) {}

    @Override
    public void onError(int code, String desc) {}
}
