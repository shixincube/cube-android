package cube.core.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cell.util.Utils;
import cube.common.callback.Callback;

/**
 * 回调管理
 *
 * @author LiuFeng
 * @data 2020/9/1 15:03
 */
public class CallBackManager {
    private static Map<Long, Callback> callbackMap = new ConcurrentHashMap<>();

    /**
     * 获取普通回调
     * 备注：只能获取一次，不能重复获取！！！
     *
     * @param sn
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T extends Callback> T getCallBack(Long sn) {
        if (sn != null) {
            return (T) callbackMap.remove(sn);
        }

        return null;
    }

    /**
     * 获取保持回调
     * 备注：可多次获取
     *
     * @param sn
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T extends Callback> T getKeepCallback(Long sn) {
        if (sn != null) {
            return (T) callbackMap.get(sn);
        }

        return null;
    }

    /**
     * 添加回调
     *
     * @param callback
     * @return
     */
    public static Long addCallBack(Callback callback) {
        Long sn = Utils.generateSerialNumber();
        return addCallBack(sn, callback);
    }

    /**
     * 添加回调
     *
     * @param sn
     * @param callback
     * @return
     */
    public static Long addCallBack(Long sn, Callback callback) {
        if (sn != null && callback != null) {
            callbackMap.put(sn, callback);
        }

        return sn;
    }

    /**
     * 移除回调
     *
     * @param sn
     */
    public static void removeCallBack(Long sn) {
        callbackMap.remove(sn);
    }

    /**
     * 清空回调
     */
    public static void clear() {
        callbackMap.clear();
    }
}
