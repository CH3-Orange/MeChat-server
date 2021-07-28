package com.example.orangepi.me;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Helper {

    public static HashMap<String,String> GetAllUser(){
        HashMap<String,String> users=new HashMap<>();
        try(Connection conn = DriverManager.getConnection(JDBCHelper.JDBCUrl,JDBCHelper.JDBCUser,JDBCHelper.JDBCPassword);
            PreparedStatement ps = conn.prepareStatement("SELECT uid,uname FROM Users ");
        ){
            try(ResultSet result =ps.executeQuery()){
                while(result.next()){
                    Long uid=result.getLong("uid");
                    String uname=result.getString("uname");
                    users.put(uid.toString(),uname);
                }
            }
        }catch(Exception ee){
            System.out.println(ee.getMessage());
        }
        return users;
    }

    public static List<HashMap<String,String>> GetAllUserWithGroup(){
        List<HashMap<String,String>> groups= new ArrayList<>();
        try(Connection conn = DriverManager.getConnection(JDBCHelper.JDBCUrl,JDBCHelper.JDBCUser,JDBCHelper.JDBCPassword);
            PreparedStatement ps = conn.prepareStatement("SELECT uid,gid FROM UserInGroup ");
        ){
            try(ResultSet result =ps.executeQuery()){
                while(result.next()){
                    Long uid=result.getLong("uid");
                    Long gid=result.getLong("gid");
                    HashMap<String,String> user=new HashMap<>();
                    user.put(gid.toString(),uid.toString());
                    groups.add(user);
                }
            }
        }catch(Exception ee){
            System.out.println(ee.getMessage());
        }
        return groups;
    }


}
