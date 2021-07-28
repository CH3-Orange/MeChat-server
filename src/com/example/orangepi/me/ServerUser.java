package com.example.orangepi.me;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerUser {
    UsersMap usersMap = new UsersMap();
    GroupMap groupMap = new GroupMap();


    public enum UserStat {
        ONLINE, OUTLINE, ERR
    }

    public void PushUsers() {
        Map<String, String> users = Helper.GetAllUser();//获取已注册用户
        for (Map.Entry<String, String> user : users.entrySet()) {
            usersMap.pushUser(new User(user.getKey(), user.getValue()), UserStat.OUTLINE);
            System.out.println("addUser:" + user.getKey() + " " + user.getValue());
        }
    }

    public void PushGroups() {
        List<HashMap<String, String>> groups = Helper.GetAllUserWithGroup();
        for (HashMap<String, String> user : groups) {
            for (Map.Entry<String, String> pair : user.entrySet()) {
                groupMap.pushGroup(pair.getKey(), pair.getValue());
                System.out.println("addGroup:" + pair.getKey() + " " + pair.getValue());
            }

        }

        for (Map.Entry<String, List<String>> gm : groupMap.Groups.entrySet()) {
            System.out.println("群组id：" + gm.getKey());
            for (String uid : gm.getValue()) {
                System.out.println("\t" + uid);
            }
        }
    }

    public void Start() {
        System.out.println("started!");
        try {
            InetAddress addr = Inet4Address.getLocalHost();
            System.out.println(addr.getHostAddress());
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        PushUsers();
        PushGroups();

        User user = new User("36", "hwBot");//hwBot 启动！
        Bot cu = new Bot(user);
        new Thread(cu).start();

        try (ServerSocket ss = new ServerSocket(10083)) {

            while (true) {
                Socket socket = ss.accept();
                System.out.println("connected! " + socket.getRemoteSocketAddress());
                Thread newConnection = new SeverHandler(socket);
                newConnection.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ServerUser su = new ServerUser();
        su.Start();

    }

    private class UsersMap {
        private Map<String, User> users;
        private Map<String, UserStat> userStat;//用户状态

        public UsersMap() {
            this.users = new HashMap<String, User>();
            this.userStat = new HashMap<String, UserStat>();
        }

        public boolean pushUser(User user, UserStat stat) {
            if (users.containsKey(user.Id)) {
                return false;
            }
            users.put(user.Id, user);
            user.reNewMessageQueue();
            userStat.put(user.Id, stat);
            return true;
        }

        public User GetUser(String userId) {
            if (users.containsKey(userId)) {//如果map中有该用户则返回用户对象
                return users.get(userId);
            }
            return null;
        }

        public UserStat GetUserStat(String userId) {
            if (userStat.containsKey(userId)) {
                return userStat.get(userId);
            }
            return UserStat.ERR;
        }

        public boolean ChangeUserStat(String userId, UserStat stat) {
            if (userStat.containsKey(userId)) {
                userStat.put(userId, stat);
                System.out.println(usersMap.getAllOnlineUses());
                return true;
            }
            return false;
        }

        public boolean PushUserMessage(String userId, UserMessage message) {
            UserStat stat = GetUserStat(userId);
            User user = GetUser(userId);
            if (user == null) return false;
            return user.PushMessage(message);
        }

        public UserMessage GetUserMessage(String userId) {
            User user = GetUser(userId);
            if (user == null) return null;
            return user.GetMessage();
        }

        public String getAllOnlineUses() {
            StringBuilder ans = new StringBuilder();
            for (Map.Entry<String, UserStat> userstat : userStat.entrySet()) {
                if (userstat.getValue() == UserStat.ONLINE) {
                    ans.append(userstat.getKey());
                    ans.append(" ");
                }
            }
            return ans.toString();
        }
    }

    private class GroupMap {
        private Map<String, List<String>> Groups;

        public GroupMap() {
            Groups = new HashMap<>();
        }

        public boolean isInGroupMap(String targetId) {
            return Groups.containsKey(targetId);
        }

        public boolean isGroupMessage(UserMessage.MsgType type) {
            return type == UserMessage.MsgType.GROUP;
        }

        public void pushGroup(String gid, String uid) {
            if (!Groups.containsKey(gid)) {//如果组不存在
                ArrayList<String> arrayList = new ArrayList();
                arrayList.add(uid);
                Groups.put(gid, arrayList);
            } else {
                List<String> arrayList = Groups.get(gid);
                if (!arrayList.contains(uid)) {//避免重复添加
                    arrayList.add(uid);
                }
            }
        }

        public List<String> GetUserInGroup(String gid) {
            if (Groups.containsKey(gid)) {
                return Groups.get(gid);//获取群聊中所有用户UID

            }
            return null;
        }

        public boolean PushUserInGroup(String gid, String uid) {
            if (!Groups.containsKey(gid)) {//如果组不存在
                return false;
            } else {
                List<String> arrayList = Groups.get(gid);
                if (!arrayList.contains(uid)) {//避免重复添加
                    arrayList.add(uid);
                    return true;
                }
                return false;//重复添加
            }
        }


    }

    private class SeverHandler extends Thread {
        Socket socket;
        User user;
        boolean ifOut = false;

        public SeverHandler(Socket s) {
            socket = s;
        }

        public void handleMessage(UserMessage msg) {
            System.out.println("tar=" + msg.TargetId + "src=" + msg.SourceId + "msg=" + (String) msg.Message);
            if (groupMap.isGroupMessage(msg.Type) && !groupMap.isInGroupMap(msg.TargetId)) {//先判断是群组消息,但不在内存中的群组列表中
                PushGroups();//刷新群列表
            }
            if (groupMap.isGroupMessage(msg.Type)) {//判断是否是群组消息
                List<String> targetUser = groupMap.GetUserInGroup(msg.TargetId);
                if (targetUser == null) {
                    System.out.println("群聊不包含任何用户: " + msg.TargetId);
                    return;
                }
                boolean isself = false;
                for (String uid : targetUser) {
                    if (uid.equals(msg.SourceId)) {
                        isself = true;
                        continue;//跳过消息发送者
                    }
                    usersMap.PushUserMessage(uid, msg);//给群聊中的用户逐个发送消息
                }
                if (!isself) {//说明内存中的群列表还不包括该用户
                    groupMap.PushUserInGroup(msg.TargetId, msg.SourceId);//将用户加入内存中的群组列表
                }
            } else {//如果还不在群列表则说明不是群消息 是单聊消息
                User targetUser = usersMap.GetUser(msg.TargetId);
                if (targetUser == null) {
                    System.out.println("不存在的用户: " + msg.TargetId);
                    return;
                }
                usersMap.PushUserMessage(targetUser.Id, msg);//单聊的消息推送
            }
            System.out.println("处理了来自" + msg.SourceId + "的消息");
        }

        @Override
        public void run() {
            try {
                handle(socket.getInputStream(), socket.getOutputStream());
            } catch (IOException | InterruptedException e) {
                System.out.print("handle: ");
                e.printStackTrace();
            }

        }

        private void handle(InputStream is, OutputStream os) throws IOException, InterruptedException {
            ObjectInputStream reader = new ObjectInputStream(is);
            ObjectOutputStream writer = new ObjectOutputStream(os);
            Thread waitNewMessage = new Thread(new Runnable() {
                @Override
                public void run() {//处理接收消息的线程
                    UserMessage acceptMessage;
                    while (!ifOut) {
                        try {
                            Mail mail = (Mail) reader.readObject();
                            if (mail == null) continue;
                            System.out.println("accept a mail");
                            switch (mail.type) {
                                case USER://如果发送的是用户信息
                                    user = (User) mail.msg;
                                    if (usersMap.GetUser(user.Id) == null) {
                                        System.out.println("重新导入用户组");
                                        PushUsers();
                                        if (usersMap.GetUser(user.Id) == null) {
                                            writer.writeObject(new Mail(Mail.TYPE.STR, "无此用户"));
                                        } else {
                                            writer.writeObject(new Mail(Mail.TYPE.STR, "登录成功"));
                                            usersMap.ChangeUserStat(user.Id, UserStat.ONLINE);//修改为在线状态
                                            System.out.println("login! wellcome:" + user.Id + " ");
                                        }
                                    } else {
                                        writer.writeObject(new Mail(Mail.TYPE.STR, "登录成功"));
                                        usersMap.ChangeUserStat(user.Id, UserStat.ONLINE);//修改为在线状态
                                        System.out.println("login! wellcome:" + user.Id + " ");
                                    }
                                    break;
                                case MESSAGE:
                                    System.out.println("Get Message!");
                                    acceptMessage = (UserMessage) mail.msg;
                                    handleMessage(acceptMessage);//处理消息信息
                                    break;
                                case STR:
                                    String str = (String) mail.msg;
                                    if (str.equals("online_users")) {//返回在线的用户uid
                                        String online_users = usersMap.getAllOnlineUses();
                                        System.out.println(" online_users " + online_users);
                                        writer.writeObject(new Mail(Mail.TYPE.STR, " online_users " + online_users));
                                    }
                                case BYE:
                                    if (user != null) {
                                        usersMap.ChangeUserStat(user.Id, UserStat.OUTLINE);
                                    }
                                    ifOut = true;
                                    writer.writeObject(new Mail(Mail.TYPE.STR, "登出成功"));
                                    reader.close();
                                    writer.close();
                                    socket.close();
                                    return;
                            }

                        } catch (ClassNotFoundException | IOException e) {
                            e.printStackTrace();
                            usersMap.ChangeUserStat(user.Id, UserStat.OUTLINE);//修改为离线状态
                            System.out.println(user.Id + "已经离线");
                            break;
                        } catch (Exception ee) {
                            System.out.println(ee.getMessage());
                            usersMap.ChangeUserStat(user.Id, UserStat.OUTLINE);//修改为离线状态
                            System.out.println(user.Id + "已经离线");
                            break;
                        }
                    }
                }
            });
            waitNewMessage.start();
            while (!ifOut) {//发送消息的线程
                if (user == null) {
                    //System.out.println("user null ");
                    Thread.sleep(10);
                    continue;//这里不加判断会产生因为user更新异步而导致的访问空指针
                }
                try {
                    //System.out.println("尝试获取消息");
                    UserMessage sendMessage = usersMap.GetUserMessage(user.Id);//从当前用户的消息池里获取消息
                    if (sendMessage != null) {
                        writer.writeObject(new Mail(Mail.TYPE.MESSAGE, sendMessage));
                        System.out.println("socket发送: " + (String) sendMessage.Message);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ifOut = true;
                    usersMap.ChangeUserStat(user.Id, UserStat.OUTLINE);//修改为离线状态
                    System.out.println(user.Id + "已经离线");
                    break;
                }
            }
        }
    }


}
