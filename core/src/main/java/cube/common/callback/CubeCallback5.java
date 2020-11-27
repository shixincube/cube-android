package cube.common.callback;

/**
 * 5个参数回调接口
 *
 * @author LiuFeng
 * @data 2020/7/16 11:08
 */
public interface CubeCallback5<T1, T2, T3, T4, T5> extends Callback {

    /**
     * 回调成功
     *
     * @param result1
     * @param result2
     * @param result3
     * @param result4
     * @param result5
     */
    void onSuccess(T1 result1, T2 result2, T3 result3, T4 result4, T5 result5);
}
