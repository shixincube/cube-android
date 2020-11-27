package cube.utils.log;

import android.util.Log;

import java.util.Arrays;

/**
 * 默认日志处理器--打印到控制台
 *
 * @author LiuFeng
 * @data 2018/9/20 11:43
 */
public class DefaultLogHandle extends BaseLogHandle {

    @Override
    public void log(LogEvent logEvent) {
        buffer.append("[");
        buffer.append(logEvent.level.name());
        buffer.append("] ");
        buffer.append(getDateTime(logEvent.timestamp));
        buffer.append(" ");
        buffer.append(getStackTrace(logEvent.threadName, logEvent.stackTrace, 5));
        buffer.append(" ");
        buffer.append(logEvent.tag);
        buffer.append(" ");
        buffer.append(logEvent.message);

        print(logEvent.level.getCode(), TAG, buffer.toString());

        buffer.delete(0, buffer.length());
    }

    /**
     * 控制台打印日志
     *
     * @param priority
     * @param tag
     * @param content
     */
    private void print(int priority, String tag, String content) {
        // 经测试log控制台最多打印4039个字节
        // 中文汉字在utf-8中一般占3个字节，即1346个汉字
        if (content.length() <= 1300) {
            Log.println(priority, tag, content);
            return;
        }

        // 最大字节数
        int maxByteNum = 4000;
        byte[] bytes = content.getBytes();

        // 字节数4000以内直接打印
        if (bytes.length <= maxByteNum) {
            Log.println(priority, tag, content);
            return;
        }

        int count = 1;
        while (bytes.length > maxByteNum) {
            // 截取字节再转字符串
            String subStr = new String(Arrays.copyOfRange(bytes, 0, maxByteNum + 2));

            // 实际截取长度减2，因为汉字一般3个字节，
            // 截取可能把一个汉字拆分成1或2个字符，减掉2个字符保证打印字符完整
            int realSubLength = subStr.length() - 2;

            String desc = String.format("分段打印(%s):%s", count++, content.substring(0, realSubLength));
            Log.println(priority, tag, desc);

            // 截取赋值还未打印字符串
            content = content.substring(realSubLength);
            bytes = content.getBytes();

            // 添加一个限制，避免有超大日志一直打印
            if (count == 10) {
                break;
            }
        }

        // 打印剩余部分
        Log.println(priority, tag, String.format("分段打印(%s):%s", count, content));
    }
}
