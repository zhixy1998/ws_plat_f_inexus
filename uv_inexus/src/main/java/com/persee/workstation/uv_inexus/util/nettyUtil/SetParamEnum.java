package com.persee.workstation.uv_inexus.util.nettyUtil;

import lombok.Getter;

@Getter
public enum SetParamEnum {


    SetWLampStatus("SetWLampStatus", "设置钨灯状态"),
    SetDLampStatus("SetDLampStatus", "设置氘灯状态"),
    SetLampPos("SetLampPos", "设置灯位置"),
    SetChangeLampWl("SetChangeLampWl", "设置换灯波长"),
    SetWlList("SetWlList", "设置波长校正列表"),
    SetScanWlInterval("SetScanWlInterval", "设置扫描间隔"),
    SetScanSpeed("SetScanSpeed", "设置扫描速度"),
    SetStartWl("SetStartWl", "设置扫描开始波长"),
    SetEndWl("SetEndWl", "设置扫描结束波长"),
    SetFilter("SetFilter", "设置滤色片"),
    SetGain("SetGain", "设置增益"),
    SetResponseTime("SetResponseTime", "设置响应时间"),
    SetPhotoMode("SetPhotoMode", "设置光度模式"),
    SetSlit1("SetSlit1", "设置主狭缝的宽度"),
    SetCellPos("SetCellPos", "设置样品池位置"),
    BaseLine("BaseLine", "基线"),
    Zero("Zero", "校零"),
    BaseLineFast("BaseLineFast", "极快基线"),
    SetPCModeTrue("SetPCModeTrue", "设置成PC模式"),
    SetPCModeFalse("SetPCModeFalse", "设置成PC模式"),
    SetProgrammeModeTrue("SetProgrammeModeTrue", "设置成程序模式"),
    SetProgrammeModeFalse("SetProgrammeModeFalse", "取消程序模式"),
    SetInitStatus("SetInitStatus", "设置初始化状态"),
    Scan("Scan", "扫描"),
    SetWl("SetWl", "设置波长");

    private final String comId;
    private final String description;

    // 构造函数
    SetParamEnum(String comId, String description) {
        this.comId = comId;
        this.description = description;
    }

    // 获取comId
    public String getComId() {
        return comId;
    }

    // 获取description
    public String getDescription() {
        return description;
    }

}
