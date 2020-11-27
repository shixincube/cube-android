package cube.utils;

import android.content.Context;
import android.os.Build;
import android.util.TypedValue;

import androidx.annotation.RequiresApi;

/**
 * 设备工具类
 *
 * @author PengZhenjin
 * @date 2016/5/26
 */
public class DeviceUtil {

    /**
     * 获取手机制造商
     *
     * @return
     */
    public static String getPhoneManufacturer() {
        return Build.MANUFACTURER;
    }

    /**
     * 获取手机品牌
     *
     * @return
     */
    public static String getPhoneBrand() {
        return Build.BRAND;
    }

    /**
     * 获取手机型号
     *
     * @return
     */
    public static String getPhoneModel() {
        return Build.MODEL;
    }

    /**
     * 获取手机CPU
     *
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static String[] getPhoneCPU() {
        return Build.SUPPORTED_ABIS;
    }

    /**
     * 获取手机Android API等级（如：22、23 ...）
     *
     * @return
     */
    public static int getBuildLevel() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * 获取手机Android版本（如：4.4、5.0、5.1 ...）
     *
     * @return
     */
    public static String getBuildVersion() {
        return Build.VERSION.RELEASE;
    }

    private static String[] huaweiRongyao = {
        "hwH60",    //荣耀6
        "hwPE",     //荣耀6 plus
        "hwH30",    //3c
        "hwHol",    //3c畅玩版
        "hwG750",   //3x
        "hw7D",      //x1
        "hwChe2",      //x1
    };

    private static String[] SanXing = { "SM-N9200" };

    public static String getDeviceInfo() {
        String handSetInfo = "手机型号：" + Build.DEVICE + "\n系统版本：" + Build.VERSION.RELEASE + "\nSDK版本：" + Build.VERSION.SDK_INT;
        return handSetInfo;
    }

    private static String getDeviceModel() {
        return Build.DEVICE;
    }

    public static boolean isHuaWeiRongyao() {
        for (String aHuaweiRongyao : huaweiRongyao) {
            if (aHuaweiRongyao.equals(getDeviceModel())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSanXingNot5() {
        for (String aSanXing : SanXing) {
            if (aSanXing.equals(getPhoneModel())) {
                return true;
            }
        }
        return false;
    }

    /**
     * dp转px
     *
     * @param context
     *
     * @return
     */

    public static int dp2px(Context context, float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal, context.getResources().getDisplayMetrics());
    }

    /**
     * sp转px
     *
     * @param context
     *
     * @return
     */
    public static int sp2px(Context context, float spVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spVal, context.getResources().getDisplayMetrics());
    }

    /**
     * px转dp
     *
     * @param context
     * @param pxVal
     *
     * @return
     */

    public static float px2dp(Context context, float pxVal) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (pxVal / scale);
    }

    /**
     * px转sp
     *
     * @param pxVal
     *
     * @return
     */
    public static float px2sp(Context context, float pxVal) {
        return (pxVal / context.getResources().getDisplayMetrics().scaledDensity);
    }
}
