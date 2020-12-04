package cube.message.impl;

import cube.core.Hook;

/**
 * 消息钩子
 *
 * @author LiuFeng
 * @data 2020/12/4 21:20
 */
public class MessageHook extends Hook {
    public static final String NAME = "MessageHook";

    public MessageHook() {
        super(NAME);
    }
}
