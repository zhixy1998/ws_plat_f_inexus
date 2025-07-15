package com.persee.workstation.uv_inexus.util.changeUtil;

import lombok.Getter;

@Getter
public enum FindDevEnum {
//    "SearchAround","NewIP" ,"Connect","GetSn","GetAccessoryType","GetInitStatus"
    FindDevice(1,"SearchAround"),//, "广播查找设备"
    NewIP(2, "NewIP"),//, "设置IP"
    Connect(3, "Connect"),//, "连接设备"
    GetSn(4, "GetSn"),//, "获取设备SN"
    GetAccessoryType(5,"GetAccessoryType"), //, "获取设备配件类型"
    GetInitStatus(6, "GetInitStatus");//, "获取设备初始化状态"

    // 获取comId
    private final String comId;
    // 获取description
    private final Integer num;

    // 构造函数
    FindDevEnum(Integer num, String comId) {
        this.comId = comId;
        this.num = num;
    }
}
