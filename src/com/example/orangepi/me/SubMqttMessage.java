package com.example.orangepi.me;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SubMqttMessage {
    String topic;
    String content;
    String broker;
    MemoryPersistence persistence;
    String clientId;
    MQTTCallBack callBack= new MQTTCallBack();
    MqttClient client=null;

    private final int MES_WAIT=0,MES_RECV=1,MES_LOST=-1;

    public SubMqttMessage(String topic, String broker) {
        this.topic = topic;
        this.broker = broker;
        clientId = "subClient";
        persistence = new MemoryPersistence();
    }
    public SubMqttMessage(String topic) {
        this.topic = topic;
        this.broker = "tcp://***.***.***.***:****";
        clientId = "subClient";
        persistence = new MemoryPersistence();
    }

    public String ReceiveMqttMessageOnce()  {
        try {
            // host为主机名，test为clientid即连接MQTT的客户端ID，一般以客户端唯一标识符表示，MemoryPersistence设置clientid的保存形式，默认为以内存保存
            client = new MqttClient(broker, clientId, persistence);
            // MQTT的连接设置
            MqttConnectOptions options = new MqttConnectOptions();
            // 设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
            options.setCleanSession(true);
//            // 设置连接的用户名
//            options.setUserName(userName);
//            // 设置连接的密码
//            options.setPassword(passWord.toCharArray());
            // 设置超时时间 单位为秒
            options.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            options.setKeepAliveInterval(20);
            // 设置回调函数
            client.setCallback(callBack);
            client.connect(options);
            //订阅消息
            client.subscribe(topic, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int cnt=0;
        while (callBack.getStatus()==MES_WAIT&&cnt<100) {//最多等10秒
            //System.out.println(callBack.status);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cnt++;
        }
        try {
            client.disconnect();
            client.close();
        } catch (MqttException e) {
            e.printStackTrace();
        }

        String str=callBack.getResult();
        System.out.println("Receive: "+cnt+" "+callBack.getStatus()+" "+str);
        return str;
    }



    private class MQTTCallBack implements MqttCallback {
        private int status=MES_WAIT;
        Lock lock= new ReentrantLock();
        private String result;

        public int getStatus() {
            lock.lock();
            int s=status;
            lock.unlock();
            return s;
        }

        public void setStatus(int status) {
            lock.lock();
            this.status = status;
            lock.unlock();
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public void connectionLost(Throwable cause) {
            System.out.println("connectionLost");
            setStatus(MES_LOST);
        }

        public void messageArrived(String topic, MqttMessage message) throws Exception {
            System.out.println("topic:" + topic);
            System.out.println("Qos:" + message.getQos());
            String res=new String(message.getPayload());
            System.out.println("message content:" + res);
            result=res;
            setStatus(MES_RECV);

        }

        public void deliveryComplete(IMqttDeliveryToken token) {
            System.out.println("deliveryComplete---------" + token.isComplete());
        }
    }
}
