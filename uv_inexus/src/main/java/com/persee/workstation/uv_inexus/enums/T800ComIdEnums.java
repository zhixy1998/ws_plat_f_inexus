package com.persee.workstation.uv_inexus.enums;

import lombok.Getter;

@Getter
public enum T800ComIdEnums {
    Search("Search","查找设备"),
    NewIP("NewIP","设置IP"),
    SetWLampStatus("SetWLampStatus","设置钨灯状态"),
    SetDLampStatus("SetDLampStatus","设置氘灯状态"),
    SetLampPos("SetLampPos","设置灯位置"),
    SetScanWlInterval("SetScanWlInterval","设置扫描间隔"),
    SetScanSpeed("SetScanSpeed","设置扫描速度"),
    SetWl("SetWl","定位波长"),
    GotoMLamp("GotoMLamp","汞灯定位"),
    SetStartWl("SetStartWl","设置扫描开始波长"),
    SetEndWl("SetEndWl","设置扫描结束波长"),
    SetFilter("SetFilter","设置滤色片"),
    SetGain("SetGain","设置增益"),
    SetPhotoMode("SetPhotoMode","设置光度模式(Abs:1,T%2,Es:3,Er:4)"),
    SetSlit1("SetSlit1","设置主狭缝的宽度"),
    SetCellPos("SetCellPos","设置样品池位置"),
    GetCellPos("GetCellPos","读取样品池位置"),
    InitLampMotor("InitLampMotor","灯电机初始化"),
    InitFilterMotor("InitFilterMotor","滤色片初始化"),
    InitSampleCell("InitSampleCell","样品池初始化"),
    InitWlMotor("InitWlMotor","波长初始化"),
    InitSlit1Motor("InitSlit1Motor","狭缝电机初始化"),
    CheckWLamp("CheckWLamp","检查钨灯能量"),
    CheckDLamp("CheckDLamp","检查氘灯能量"),
    CheckWl("CheckWl","检查波长"),
    Stop("Stop","停止扫描"),
    Dark("Dark","暗电流校正"),
    Measure("Measure","读取测量值"),
    BaseLine("BaseLine","基线"),
    Scan("Scan","扫描"),
    GetSn("GetSn","读取序列号"),
    Connect("Connect","仪器连接"),
    SearchAround("SearchAround","能力协商"),
    Set("Set","连续设置参数例子"),
    Init("Init","初始化"),
    GetInitStatus("GetInitStatus","获取初始化状态"),
    Zero("Zero","校零"),
    SetResponseTime("SetResponseTime","设置响应时间")
    ;
    private final String comId;
    private final String Describe;
    T800ComIdEnums(String comId, String Describe) {
        this.comId = comId;
        this.Describe = Describe;
    }
}
