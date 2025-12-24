/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil.cmdEnums;

import lombok.Getter;

@Getter
public enum LiveDetectionCmdIdEnum {

//    Connect(1, "Connect", null, 2),
GetPhotoMode("GetPhotoMode"),
    GetResponseTime("GetResponseTime"),
    Measure("Measure"),
    GetChangeLampWl("GetChangeLampWl"),
    GetCellPos("GetCellPos"),
    GetLampPos("GetLampPos"),
    GetWLampStatus("GetWLampStatus"),
    GetDLampStatus("GetDLampStatus");
//    GetAccessoryType(6, "GetAccessoryType", null, 2);


    // 指令
    private final String comId;


    // 构造函数
    LiveDetectionCmdIdEnum(String comId) {
        this.comId = comId;

    }


}
