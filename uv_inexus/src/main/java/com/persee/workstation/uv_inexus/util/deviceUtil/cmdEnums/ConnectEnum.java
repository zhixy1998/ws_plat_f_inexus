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

/**
 * @author xudian
 */

@Getter
public enum ConnectEnum {
    //, "连接设备"
    // 连接设备
    Connect("Connect"),
    TestConnect("TestConnect"),
    // 获取设备SN
    GetSn("GetSn"),
    // 获取设备配件类型
    GetAccessoryType("GetAccessoryType"),
    SetCellPos("SetCellPos"),
    // 获取设备初始化状态
    GetInitStatus("GetInitStatus"),
    // 获取当前样品池位置
    GetCellPos("GetCellPos"),
    T800GetWlList("T800GetWlList"),
    // 获取波长信息
    GetWlList("GetWlList");

    // 获取comId
    private final String comId;


    // 构造函数
    ConnectEnum(String comId) {
        this.comId = comId;
    }

}
