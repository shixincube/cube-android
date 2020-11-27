package cube.common.callback;

/**
 * 简化回调接口
 *
 * @author LiuFeng
 * @data 2020/7/16 11:05
 */
public class SimpleCallback0 implements CubeCallback0 {

    @Override
    public void onSuccess() {}

    @Override
    public void onError(int code, String desc) {}
}
