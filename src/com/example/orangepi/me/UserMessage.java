package com.example.orangepi.me;

import java.io.Serializable;

public abstract class UserMessage implements Serializable,Cloneable {
    public String SourceId;
    public String TargetId;
    public Object Message;
    public MsgType Type;// Type=TEXT&SINGLE
    public enum MsgType{
        SINGLE,GROUP//单向消息，群组消息
    }
    private static final long serialVersionUID=10000L;
    public UserMessage(String  SourceId,String  TargetId,MsgType msgType){
        this.SourceId=SourceId;
        this.TargetId=TargetId;
        Type=msgType;

    }
    public UserMessage(String  SourceId,String  TargetId){
        this.SourceId=SourceId;
        this.TargetId=TargetId;
        Type=MsgType.SINGLE;//默认为单聊消息
    }
    @Override
    public Object clone(){
        Object copied=null;
        try{
            copied=(UserMessage)super.clone();
        }catch (CloneNotSupportedException ee){
            System.out.println("(Error)UserMessage:"+ee.getMessage());
        }
        return copied;
    }
    //abstract public void ReadInMessage() throws IOException;
    abstract public UserMessage ChangeDirection();
}

