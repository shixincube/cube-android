package cube.common.callback;

/**
 * 2个参数回调接口
 *
 * @author LiuFeng
 * @data 2020/7/16 11:08
 */
public interface CubeCallback2<T1, T2> extends Callback {

    /**
     * 回调成功
     *
     * @param result1
     * @param result2
     */
    void onSuccess(T1 result1, T2 result2);
}
