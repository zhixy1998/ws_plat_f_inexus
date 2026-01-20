package com.persee.workstation.uv_inexus.util.deviceUtil.param;

import lombok.Getter;

@Getter
public enum ActionCmdEnum {

    Zero("Zero", "校零"),
    Measure("Measure", "点测量"),
    Stop("Stop", "停止扫描"),

    BaseLine("BaseLine", "基线扫描"),
    Scan("Scan", "区间扫描"),
    ScanFast("ScanFast", "快速区间扫描"),
    BaseLineFast("BaseLineFast", "快速基线扫描"),
    Dark("Dark","暗电流");

    private final String cmdId;
    private final String desc;

    ActionCmdEnum(String cmdId, String desc) {
        this.cmdId = cmdId;
        this.desc = desc;
    }

}


