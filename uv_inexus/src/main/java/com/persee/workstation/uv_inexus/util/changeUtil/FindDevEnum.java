/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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
    GetInitStatus(6, "GetInitStatus"),//, "获取设备初始化状态"
    GetCellPos(7, "GetCellPos"),/// "获取当前样品池位置"
    GetWlList(8, "GetWlList"); /// "获取波长信息"

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
