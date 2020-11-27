package cube.message.service.model;

import org.json.JSONException;
import org.json.JSONObject;

import cube.message.service.MessageType;

/**
 * 文本消息
 *
 * @author LiuFeng
 * @data 2020/9/8 15:00
 */
public class TextMessage extends Message {
    private String content;

    public TextMessage() {
        super(0, MessageType.Text);
    }

    public TextMessage(String content) {
        super(0, MessageType.Text);
        this.content = content;
    }

    public TextMessage(String content, String sender, String receiver) {
        super(0, MessageType.Text, sender, receiver);
        this.content = content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return this.content;
    }

    @Override
    public JSONObject toJSON() {
        try {
            this.payload.put("content", content);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return super.toJSON();
    }
}
