package com.persee.workstation.uv_inexus.util.deviceUtil.param;

import lombok.Getter;

@Getter
public enum GetParam {
    PHOTO_MODE("photoMode", "GetPhotoMode"),
    CELL_POS("cellPos","GetCellPos"),
    CHANGE_LAMP_WL("changeLampWl","GetChangeLampWl"),
    SN("sn","GetSn"),
    INIT_STATUS("initStatus","GetInitStatus"),
    LAMP_POS("lampPos","GetLampPos"),
    WL_List("wlList","GetWlList"),
    ACCESSORY_TYPE("accessoryType","GetAccessoryType");


    private final String paramField;
    private final String cmdId;

    GetParam(String paramField, String cmdId) {
        this.paramField = paramField;
        this.cmdId = cmdId;
    }
}
