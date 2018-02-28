package com.example.shareiceboxms.models.http.mqtt;

import android.util.Log;

import com.example.shareiceboxms.models.beans.PerSonMessage;
import com.example.shareiceboxms.models.contants.HttpRequstUrl;
import com.example.shareiceboxms.views.activities.BaseActivity;
import com.example.shareiceboxms.views.activities.HomeActivity;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class GetService {
    public static GetService instance;
    public static final String HOST = "tcp://" + HttpRequstUrl.IP + ":61616";//tcp://127.0.0.1:61613
    private static MqttClient client;
    private MqttConnectOptions options;
    private String userName = "admin";
    private String passWord = "password";

    public static synchronized GetService getInstance() {
        if (instance == null) {
            instance = new GetService();
        }
        return instance;
    }

    public void start() {
        Log.d("连接中", ".......");
        init();
    }

    public void breakClient() {
        Log.d("断开", ".......");
        if (client.isConnected()) {
            try {
                client.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

    }

    public boolean isConnected() {
        if (client != null) {
            return client.isConnected();
        } else {
            return false;
        }
    }

    private void init() {
        try {
            // host为主机名，clientid即连接MQTT的客户端ID，一般以唯一标识符表示，MemoryPersistence设置clientid的保存形式，默认为以内存保存
            client = new MqttClient(HOST, String.valueOf(PerSonMessage.userId), new MemoryPersistence());
            // MQTT的连接设置
            options = new MqttConnectOptions();
            // 设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
            options.setCleanSession(true);
            // 设置连接的用户名
            options.setUserName(userName);
            // 设置连接的密码
            options.setPassword(passWord.toCharArray());
            // 设置超时时间 单位为秒
            options.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            options.setKeepAliveInterval(20);
            // 设置回调
            client.setCallback(new HomeActivity());
            MqttTopic topic = client.getTopic("000");
            // setWill方法，如果项目中需要知道客户端是否掉线可以调用该方法。设置最终端口的通知消息
            options.setWill(topic, "close".getBytes(), 2, true);
            client.connect(options);
            // 订阅消息
            int[] Qos = {2};
            String[] topic1 = {"T-M-" + PerSonMessage.userId};
            client.subscribe(topic1, Qos);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
