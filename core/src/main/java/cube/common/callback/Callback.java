package cube.common.callback;

/**
 * 基类回调接口
 * 描述：所有CubeCallback接口都必须继承自它
 *
 * @author LiuFeng
 * @data 2020/7/16 11:03
 */
public interface Callback {

    /**
     * 失败回调
     *
     * @param code
     * @param desc
     */
    void onError(int code, String desc);
}
