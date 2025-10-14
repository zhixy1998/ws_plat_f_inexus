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
public enum PhotoMetricChangeMethodTypeEnum {

    newMethod("newMethod", "将为您生成新方法...."),
    noData("noData", "当前没有文件数据，将应用该方法为您生成新文件数据。"),
    measure("measure", "检测到您修改了测量参数，将为您生成新方法新文件数据，是否保存当前未保存的数据："),
    analyze("analyze", "检测到您修改了分析参数，将为您生成新方法，请选择数据处理方式："),
    noChange("noChange", "方法参数没有变化，无需重新应用！");

    private final String identity;

    private final String message;


    PhotoMetricChangeMethodTypeEnum(String identity, String message){
        this.identity = identity;
        this.message = message;
    }

    public static String getMessage(String identity) {
        for (PhotoMetricChangeMethodTypeEnum c : PhotoMetricChangeMethodTypeEnum.values()) {
            if (c.getIdentity().equals(identity)) {
                return c.message;
            }
        }
        return null;
    }
}
