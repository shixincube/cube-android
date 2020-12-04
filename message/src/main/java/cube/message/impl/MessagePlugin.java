package cube.message.impl;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import cube.core.Plugin;
import cube.message.service.model.Message;
import cube.message.service.model.TextMessage;
import cube.utils.log.LogUtil;

/**
 * 消息插件
 * 示例:过滤敏感词
 *
 * @author LiuFeng
 * @data 2020/12/3 22:59
 */
public class MessagePlugin implements Plugin<Message, Message> {
    private static List<String> sensitiveWords = new ArrayList<>();

    static {
        sensitiveWords.add("色情");
        sensitiveWords.add("法轮功");
        sensitiveWords.add("反动");
    }

    @Override
    public Message onEvent(String name, Message data) {
        LogUtil.i("MessagePlugin --> 执行消息插件");
        if (data instanceof TextMessage) {
            TextMessage message = (TextMessage) data;
            String content = message.getContent();
            if (!TextUtils.isEmpty(content)) {
                // 判断并替换内容中的敏感词
                boolean hasSensitiveWords = false;
                for (String word : sensitiveWords) {
                    if (content.contains(word)) {
                        hasSensitiveWords = true;
                        content.replaceAll(word, "*");
                    }
                }

                if (hasSensitiveWords) {
                    String messageJson = data.toJSON().toString();
                    LogUtil.i("MessagePlugin --> 有敏感词，执行过滤：\n" + messageJson);
                }
            }
        }

        return data;
    }
}
