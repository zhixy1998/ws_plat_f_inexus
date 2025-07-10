package com.persee.workstation.uv_inexus.util.nettyUtil;

import lombok.Getter;

@Getter
public enum SetParamEnum {

    First("SetStartWl"),
    Second("SetEndWl"),
    Third("SetLampPos"),
    Fourth("SetGain"),
    Fifth("SetPhotoMode"),
    Sixth("SetScanSpeed"),
    Seventh("SetScanWlInterval");

    private final String comId;

//    private final String name;

    SetParamEnum(String comId){
        this.comId = comId;
//        this.name = name;
    }

}
