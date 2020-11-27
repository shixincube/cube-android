package cube.common.callback;

/**
 * N个参数回调接口
 * 备注：最好用具体参数接口，少用这个，避免类型强转
 *
 * @author LiuFeng
 * @data 2020/7/16 11:08
 */
public interface CubeCallbackN extends Callback {

    /**
     * 回调成功
     *
     * @param args
     */
    void onSuccess(Object... args);
}
