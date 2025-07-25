package com.persee.workstation.uv_inexus.util.changeUtil;

import lombok.Data;

import java.util.List;

@Data
public class Transition {
    private String ID;
    private String Describe;
    private String Timeout;
    private String ACK;
    private transient byte[] TypeByte;
    private List<String> InputString;
    private List<String> OutputString;
    private String[] SendBytes;
    private String[] ReadBytes;
}
