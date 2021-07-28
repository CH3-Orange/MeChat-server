package com.example.orangepi.me;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Bot implements Runnable {
    public User user;
    public Map<String,String> Quo;

    public Bot(String id, String name) {
        user = new User(id, name);
    }

    public Bot(User user) {
        this.user = user;
        Quo= new HashMap<>();
        NewQuo();
    }
    public void NewQuo(){
        //后续改成放在数据库里的形式
        Quo.put("当ta不回你的微信时，可能是他有十个课设要做。","在吗,在,在?");
        Quo.put("遇到问题，就装死。","难");
        Quo.put("头发留长一点，以后去劳改。","头发,秃头");
        Quo.put("应用层加一个hl协议，用脑电波发送信息。","发消息");
        Quo.put("国内桥洞比较多，去国外不好找，国内随便睡","桥洞");
        Quo.put("你们在三次元上课，我在二次元上课，有什么错吗？","人呢");
        Quo.put("为什么有个猪头套在人身上","说话");
        Quo.put("夹子听到你们说的都蹦不住了","bang溃,崩溃,绷不住了");
        Quo.put("在德玛西亚人人都会JAVA","Java,JAVA,java");
        Quo.put("卷惑：“寄把何在？”玮公言：“于尔胯下也。” ","寄把谁啊");
        Quo.put("我觉得浩玮语录可以印一伯本","浩玮语录");
        Quo.put("wj：我家占地，半亩","多大");
        Quo.put("怎样才能在wj家的高尔夫球场打出漂亮的曲线？","wj家");
        Quo.put("我二年级的儿子写出来都比你这强。","怎么样");
        Quo.put("反正头发都会没，活着的时候保持发量就行了。","脱发,掉发");
        Quo.put("hl：你的鱼塘无人问津？\n" +
                    "wj：确实，我的鱼塘已被忘记。\n" +
                    "玮公言：你的鱼塘还分淡季和旺季？","鱼塘");
        Quo.put("玮公言：吾鼠也。\n" +
                    "严公曰：汝即鼠，杰瑞表兄也。","鼠鼠");
        Quo.put("把鬼子带进村也是你策划的？","策划");
        Quo.put("可以，以后你进来我不用你扫码，直接放你进来。","打工");
        Quo.put("这不是我的本命课，我的本命课是保安的修炼手册。","上课");
        Quo.put("迎面走来一个女生，我的视线只剩一半。","女生");
        Quo.put("652的罪人，就要割掉头挂在652的门口。","罪人");
        Quo.put("我跟饭认识。","饭");
        Quo.put("hl：人与人的大脑是有区别的。\n" +
                "hw：确实，我的大脑光滑无比。","区别");
        Quo.put("hl不亮了，天就亮了。","天亮");
        Quo.put("我要证明是我抛弃了这个时代，而不是时 代抛弃了我，这就上床摆烂。","摆烂,躺平");
        Quo.put("卷与学妹畅谈。\n" +
                "玮公曰：汝杯几何？\n" +
                "卷复：何汝无学妹邪？汝牙显之早也！","妹子");
        Quo.put("SC河马!!","唉");
        Quo.put("看烂代码，晚上会做噩梦的。","bug");
        Quo.put("mqtt,652/led_send,Led on,652/led_recv","开灯,开一下灯");//mqtt,发送的topic名称，指令，监听的topic名称
        Quo.put("mqtt,652/led_send,Led off,652/led_recv","关灯,关一下灯");


    }

    @Override
    public void run() {
        ConnectServer();
    }

    public void ConnectServer() {
        try(Socket socket = new Socket("localhost",10083)){
            try (InputStream is = socket.getInputStream()) {
                try (OutputStream os = socket.getOutputStream()) {
                    System.out.println("connected!");
                    IOHandle(is, os);
                }
            }
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        System.out.println(user.Id + "connect closed!");

    }

    private void IOHandle(InputStream is, OutputStream os) throws Exception {
        ObjectOutputStream writer = new ObjectOutputStream(os);
        ObjectInputStream reader = new ObjectInputStream(is);
        writer.writeObject(new Mail(Mail.TYPE.USER, user));//先把自己的信息传过去
        System.out.println("自己信息已传送");
        UserMessage sendMessage;
        Thread waitNewMessage = new Thread(() -> {//接收消息线程
            Mail acceptMessage;
            while (true) {
                try {
                    //System.out.print("trying...");
                    acceptMessage = (Mail) reader.readObject();
                    if (acceptMessage != null) {
                        switch (acceptMessage.type) {
                            case MESSAGE:
                                UserMessage msg = (UserMessage) acceptMessage.msg;
                                System.out.println(msg.toString());
                                UserMessage reply=autoReply(msg);
                                if(reply!=null){
                                    user.PushMessage(reply);
                                }
                                break;
                            case STR:
                                System.out.println("接收到一条字符串 " + acceptMessage.msg);

                        }
                    } else {
                        System.out.print("accepted Message is null ");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
        waitNewMessage.start();
        while (true)//发送消息
        {
            //Thread.sleep(3000);
            try {
                sendMessage = user.GetMessage();
                if (sendMessage != null) {
                    Mail mail = new Mail(Mail.TYPE.MESSAGE, sendMessage);
                    writer.writeObject(mail);//向socket流发送消息
                    System.out.println("消息已发送");
                } else {
                    //System.out.println("no input now");
                }


            } catch (Exception ee) {
                System.out.println("main:" + ee.getMessage());
                break;
            }
        }

    }

    private UserMessage autoReply(UserMessage msg) {
        String text=msg.Message.toString();
        String re = null;
        boolean if_match=false;
        for(Map.Entry<String,String> quos:Quo.entrySet()){
            String[] matchs=quos.getValue().split(",");
            for(String match:matchs){
                if(text.contains(match)){//匹配到关键词
                    if_match=true;
                    break;
                }
            }
            if(if_match){
                re=quos.getKey();
                break;
            }
        }
        //判断re里是否含有mqtt 即发送的数据是否可以被解析为mqtt的指令
        if(re!=null&&re.contains("mqtt")){
            String[] cmds=re.split(",");
            if(cmds.length!=4){
                re="错误mqtt指令，联系管理员: "+re;
            }
            else {
                String topic_send=cmds[1],content=cmds[2],topic_recv=cmds[3];
                PubMqttMessage pubMqttMessage = new PubMqttMessage(topic_send);
                SubMqttMessage subMqtt=new SubMqttMessage(topic_recv);
                pubMqttMessage.SendMqttMessageOnce(content);
                String recv=subMqtt.ReceiveMqttMessageOnce();
                re=recv;
            }
        }


        UserMessage reply;
        switch (msg.Type){
            case GROUP:
                if(re==null)return null;//群聊中若没有匹配到则不回复
                reply=new TextMessage(user.Id, msg.TargetId,re, UserMessage.MsgType.GROUP);
                break;
            default:
                if(re==null) re="...";//单聊中若没有匹配到则回复...
                reply=new TextMessage(user.Id, msg.SourceId,re, UserMessage.MsgType.SINGLE);

        }
        return reply;
    }

    public static void main(String[] args) {
           /* for(int i=0;i<2;i++){
                Bot cu=new Bot(i+"");
                new Thread(cu).start();
            }*/

        User user = new User("36", "hwBot");
        Bot cu = new Bot(user);
        new Thread(cu).start();
    }
}
