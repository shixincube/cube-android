package cube.common.callback;

/**
 * 3个参数回调接口
 *
 * @author LiuFeng
 * @data 2020/7/16 11:08
 */
public interface CubeCallback3<T1, T2, T3> extends Callback {

    /**
     * 回调成功
     *
     * @param result1
     * @param result2
     * @param result3
     */
    void onSuccess(T1 result1, T2 result2, T3 result3);
}
