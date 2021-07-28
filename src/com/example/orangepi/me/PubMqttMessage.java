package com.example.orangepi.me;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class PubMqttMessage {
    String topic;
    String content;
    String broker;
    MemoryPersistence persistence;
    String clientId;

    public PubMqttMessage(String topic, String broker) {
        this.topic = topic;
        this.broker = broker;
        clientId = "pubClient";
        persistence = new MemoryPersistence();
    }
    public PubMqttMessage(String topic) {
        this.topic = topic;
        this.broker = "tcp://***.***.***.***:****";
        clientId = "pubClient";
        persistence = new MemoryPersistence();
    }

    public void SendMqttMessageOnce(String content){
        this.content=content;
        try {
            // 创建客户端
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            // 创建链接参数
            MqttConnectOptions connOpts = new MqttConnectOptions();
            // 在重新启动和重新连接时记住状态
            connOpts.setCleanSession(false);
//            // 设置连接的用户名
//            connOpts.setUserName(userName);
//            connOpts.setPassword(password.toCharArray());
            // 建立连接
            sampleClient.connect(connOpts);
            // 创建消息
            MqttMessage message = new MqttMessage(content.getBytes());
            // 设置消息的服务质量
            message.setQos(1);
            // 发布消息
            sampleClient.publish(topic, message);
            // 断开连接
            sampleClient.disconnect();
            // 关闭客户端
            sampleClient.close();
        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }

    }
}
