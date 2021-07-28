package com.example.orangepi.me;

import java.io.*;
import java.net.Socket;

public class ClientUser implements Runnable {
    public User user;

    public ClientUser(String id, String name) {
        user = new User(id, name);
    }

    public ClientUser(User user) {
        this.user = user;
    }

    @Override
    public void run() {
        ConnectServer();
    }

    public void ConnectServer() {
        try (Socket socket = new Socket("***.***.***.***", 00000)) {
            try (InputStream is = socket.getInputStream()) {
                try (OutputStream os = socket.getOutputStream()) {
                    System.out.println("connected!");
                    IOHandle(is, os);
                }
            }
        } catch (Exception ee) {
//            JOptionPane.showMessageDialog(null,ee.getMessage());
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

    public static void main(String[] args) {
           /* for(int i=0;i<2;i++){
                ClientUser cu=new ClientUser(i+"");
                new Thread(cu).start();
            }*/

        User user = new User(args[0], args[1]);
        ClientUser cu = new ClientUser(user);
        Thread inputThread = new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                while (true) {
                    String message = reader.readLine();
                    //System.out.println("Get input" +message);
                    String[] messages = message.split(" ");
                    if (message.equals("bye") || message.equals("Bye") || message.equals("BYE")) {
                        //user.PushMessage(new TextMessage(user.Id,messages[0],messages[1]));
                        //dosomething
                        break;
                    }
                    if (messages.length != 2) {
                        System.out.println("请输入发送者id和发送的信息哦");
                        continue;
                    }
                    //这里要改，目前暂且认为发送给小于10的id都是群组消息
                    if (Integer.parseInt(messages[0]) < 10) {//判断是发送的群组消息
                        user.PushMessage(new TextMessage(user.Id, messages[0], messages[1], UserMessage.MsgType.GROUP));
                    } else {
                        user.PushMessage(new TextMessage(user.Id, messages[0], messages[1], UserMessage.MsgType.SINGLE));
                    }
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        new Thread(cu).start();
        inputThread.start();
    }
}
