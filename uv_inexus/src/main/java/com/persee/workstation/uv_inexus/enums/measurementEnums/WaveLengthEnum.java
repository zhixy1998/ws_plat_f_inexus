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
public enum WaveLengthEnum {

    abs("1", "Abs"),
    tPercent("2", "T%"),
    es("3", "Es"),
    er("4", "Er");

    private final String code;

    private final String name;


    WaveLengthEnum(String code, String name){
        this.code = code;
        this.name = name;
    }

    public static String getName(String code) {
        for (WaveLengthEnum c : WaveLengthEnum.values()) {
            if (c.getCode().equals(code)) {
                return c.name;
            }
        }
        return null;
    }
}
