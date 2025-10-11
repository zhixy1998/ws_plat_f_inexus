package com.persee.workstation.uv_inexus.util.changeUtil.T800;

import com.persee.workstation.uv_inexus.util.nettyUtil.ProtocolltemList.ProtocolItem;

import java.util.List;

public class T800Transition extends ProtocolItem {

    private String ID;
    private String Describe;
    private String Timeout;
    private String ACK;
    private transient byte[] TypeByte;  // transient 表示序列化时忽略
    private List<String> InputString;
    private List<String> OutputString;
    private String[] SendBytes;
    private String[] ReadBytes;

    // 无参构造
    public T800Transition() {
    }

    // 全参构造
    public T800Transition(String ID, String Describe, String Timeout, String ACK, byte[] TypeByte,
                          List<String> InputString, List<String> OutputString,
                          String[] SendBytes, String[] ReadBytes) {
        this.ID = ID;
        this.Describe = Describe;
        this.Timeout = Timeout;
        this.ACK = ACK;
        this.TypeByte = TypeByte;
        this.InputString = InputString;
        this.OutputString = OutputString;
        this.SendBytes = SendBytes;
        this.ReadBytes = ReadBytes;
    }

    // Getter 和 Setter
    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getDescribe() {
        return Describe;
    }

    public void setDescribe(String Describe) {
        this.Describe = Describe;
    }

    public String getTimeout() {
        return Timeout;
    }

    public void setTimeout(String Timeout) {
        this.Timeout = Timeout;
    }

    public String getACK() {
        return ACK;
    }

    public void setACK(String ACK) {
        this.ACK = ACK;
    }

    public byte[] getTypeByte() {
        return TypeByte;
    }

    public void setTypeByte(byte[] TypeByte) {
        this.TypeByte = TypeByte;
    }

    public List<String> getInputString() {
        return InputString;
    }

    public void setInputString(List<String> InputString) {
        this.InputString = InputString;
    }

    public List<String> getOutputString() {
        return OutputString;
    }

    public void setOutputString(List<String> OutputString) {
        this.OutputString = OutputString;
    }

    public String[] getSendBytes() {
        return SendBytes;
    }

    public void setSendBytes(String[] SendBytes) {
        this.SendBytes = SendBytes;
    }

    public String[] getReadBytes() {
        return ReadBytes;
    }

    public void setReadBytes(String[] ReadBytes) {
        this.ReadBytes = ReadBytes;
    }
}
