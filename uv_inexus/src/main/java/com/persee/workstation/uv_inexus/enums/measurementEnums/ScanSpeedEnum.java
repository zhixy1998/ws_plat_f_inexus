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
public enum ScanSpeedEnum {

//    ["慢速", "中速", "快速", "极快"]
    SLOW("0", "慢速"),
    MEDIUM("1", "中速"),
    FAST("2", "快速"),
    SUPER_FAST("3", "极快");


    private final String code;

    private final String name;


    ScanSpeedEnum(String code, String name){
        this.code = code;
        this.name = name;
    }

    public static String getCodeByName(String name) {
        for (ScanSpeedEnum c : ScanSpeedEnum.values()) {
            if (c.getName().equals(name)) {
                return c.code;
            }
        }
        return null;
    }
}
