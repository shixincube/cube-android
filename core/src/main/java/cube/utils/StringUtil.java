package cube.utils;

/**
 * 字符串处理工具
 *
 * @author LiuFeng
 * @date 2018-8-30
 */
public class StringUtil {
    /**
     * 获取指定数量空格
     *
     * @param spaces
     *
     * @return
     */
    public static String getBlank(int spaces) {
        String number = spaces <= 0 ? "" : String.valueOf(spaces);
        return String.format("%" + number + "s", "");
    }
}
