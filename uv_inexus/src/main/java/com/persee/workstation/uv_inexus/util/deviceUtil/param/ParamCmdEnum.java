package com.persee.workstation.uv_inexus.util.deviceUtil.param;

import lombok.Getter;

@Getter
public enum ParamCmdEnum {

    PHOTO_MODE("photoMode", "SetPhotoMode"), //前端传过来的参数字段，通信协议里的命令ID
    SCAN_SPEED("scanSpeed", "SetScanSpeed"),
    START_WL("startWl", "SetStartWl"),
    END_WL("endWl", "SetEndWl"),
    SCAN_WL_INTERVAL("scanWlInterval", "SetScanWlInterval"),
    GAIN("gain", "SetGain"),
    SLIT1("slit1", "SetSlit1"),
    LAMP_POS("lampPos", "SetLampPos"),
    CORR_STATUS("corrStatus", "DoIt"),
    WL_LAMP("mlLamp", "MLamp"),
    WLamp_Status("WLampStatus", "SetWLampStatus"),
    DLamp_Status("DLampStatus", "SetDLampStatus"),
    Change_LampWl("changeLampWl", "SetChangeLampWl"),
    Wl_List("wlList", "SetWlList"),
    Set_Wl("setWl", "SetWl"),
    Filter("filter", "SetFilter"),
    Response_Time("responseTime", "SetResponseTime"),
    Cell_Pos("cellPos", "SetCellPos"),
    Dark("dark", "Dark"),
    Init_Status("initStatus", "SetInitStatus");

    private final String paramField;
    private final String cmdId;

    ParamCmdEnum(String paramField, String cmdId) {
        this.paramField = paramField;
        this.cmdId = cmdId;
    }

}

