package com.persee.workstation.uv_inexus.util.nettyUtil;

import java.util.Arrays;
import java.util.List;

public class CmdConstant {


    public static final List<String> connect = Arrays.asList("SearchAround","NewIP" ,"Connect","GetSn","GetAccessoryType","GetInitStatus");


    public static final List<String> init = Arrays.asList("GetAccessoryType", "GetInitStatus", "InitSampleCell", "SetCellPos", "InitFilterMotor", "InitSlit1Motor", "SetSlit1", "InitWlMotor", "InitLampMotor", "CheckWLamp", "CheckDLamp", "CheckWl");


    public static final List<String> setParam = Arrays.asList("GetAccessoryType", "GetInitStatus", "InitSampleCell", "SetCellPos", "InitFilterMotor", "InitSlit1Motor", "SetSlit1", "InitWlMotor", "InitLampMotor", "CheckWLamp", "CheckDLamp", "CheckWl");

}
