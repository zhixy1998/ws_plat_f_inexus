/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.enums.measurementEnums;

import lombok.Getter;

@Getter
public enum RationChangeMethodTypeEnums {

    newMethod("newMethod", "将为您生成新方法...."),
    noData("noData", "当前没有文件数据，将应用该方法为您生成新文件数据。"),
    measure("measure", "您已修改测量参数，将生成新方法并清理测量和分析数据，是否要保存现有数据？"),
    curve("curve", "您已修改曲线相关参数，将生成新方法并清理分析数据，是否要保存现有数据？"),
    analyze("analyze", "您已修改分析参数，将生成新方法并清理分析数据，是否保存现有数据？"),
    noChange("noChange", "方法参数没有变化，无需重新应用！");

    private final String identity;

    private final String message;


    RationChangeMethodTypeEnums(String identity, String message){
        this.identity = identity;
        this.message = message;
    }

    public static String getMessage(String identity) {
        for (RationChangeMethodTypeEnums c : RationChangeMethodTypeEnums.values()) {
            if (c.getIdentity().equals(identity)) {
                return c.message;
            }
        }
        return null;
    }

}
