package com.test;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import cube.common.callback.CubeCallback1;
import cube.contact.service.Contact;
import cube.contact.service.ContactService;
import cube.contact.service.Self;
import cube.core.KernelConfig;
import cube.engine.CubeEngine;
import cube.message.service.MessageListener;
import cube.message.service.MessageService;
import cube.message.service.MessageType;
import cube.message.service.model.Message;
import cube.message.service.model.TextMessage;
import cube.service.model.CubeConfig;
import cube.service.model.DeviceInfo;
import cube.utils.SpUtil;
import cube.utils.log.LogUtil;

public class MainActivity extends AppCompatActivity implements MessageListener {
    private EditText receiverEdt;
    private EditText contentEdt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        receiverEdt = findViewById(R.id.receiver_edt);
        contentEdt = findViewById(R.id.message_edt);

        findViewById(R.id.content_tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initEngine();
            }
        });


        findViewById(R.id.self_tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String account = ((EditText) findViewById(R.id.account_edt)).getText().toString();
                Self self = new Self(Long.valueOf(account), "李白");
                CubeEngine.getInstance().getService(ContactService.class).setSelf(self, new CubeCallback1<Contact>() {
                    @Override
                    public void onSuccess(Contact result) {
                        LogUtil.i("setSelf --> result:" + result);
                        Toast.makeText(MainActivity.this, "账号设置成功", Toast.LENGTH_LONG).show();

                        SpUtil.setCubeId(account);
                        SpUtil.setDisplayName(self.getName());
                        String avatar = "https://dss3.bdstatic.com/70cFv8Sh_Q1YnxGkpoWK1HF6hhy/it/u=2534506313,1688529724&fm=26&gp=0.jpg";
                        SpUtil.setAvatar(avatar);
                        SpUtil.setDebug(true);
                    }

                    @Override
                    public void onError(int code, String desc) {
                        LogUtil.i("setSelf --> onError code:" + code + " desc:" + desc);
                    }
                });
            }
        });


        findViewById(R.id.send_tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String receiverId = receiverEdt.getText().toString();
                String content = contentEdt.getText().toString();

                TextMessage message = new TextMessage();
                message.setTo(receiverId);
                message.setContent(SpUtil.getCubeId()+ " : " + content);
                CubeEngine.getInstance().getService(MessageService.class).sendMessage(message, new CubeCallback1<Message>() {
                    @Override
                    public void onSuccess(Message result) {
                        LogUtil.i("sendMessage --> result:" + result);
                        Toast.makeText(MainActivity.this, "发送消息：" + message.getContent(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(int code, String desc) {
                        LogUtil.i("sendMessage --> onError code:" + code + " desc:" + desc);
                        Toast.makeText(MainActivity.this, "发生失败：" + desc, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void initEngine() {
        KernelConfig config = new KernelConfig();
        config.address = "192.168.1.113";
        config.port = 7000;
        config.domain = "shixincube.com";
        config.appKey = "shixin-cubeteam-opensource-appkey";
        CubeEngine.getInstance().startup(this, config);

        CubeEngine.getInstance().getService(MessageService.class).addListener(this);
    }

    @Override
    public void onMessageSent(Message message) {

    }

    @Override
    public void onMessageReceived(Message message) {
        MessageType messageType = message.getType();
        if (messageType == MessageType.Text) {
            TextMessage textMessage = (TextMessage) message;
            Toast.makeText(MainActivity.this, "收到消息："+ textMessage.getContent(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onMessageRecalled(Message message) {

    }

    @Override
    public void onMessageReceipted(String sessionId, long timestamp, DeviceInfo deviceInfo) {

    }

    @Override
    public void onMessagesSync(Map<String, List<Message>> messageMap) {

    }

    @Override
    public void onMessageError(int code, String desc) {

    }
}