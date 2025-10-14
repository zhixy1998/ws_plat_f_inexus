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
public enum SampleTypeEnum {
    empty("empty", "空白样品"),
    stand("stand", "标准样品"),
    unKnown("unknown", "未知样品"),
    quality("quality", "质控样品"),
    kind_stand("0", "标准样品"),
    kind_unknown("1", "未知样品");

    private final String identity;

    private final String name;


    SampleTypeEnum(String identity, String name){
        this.identity = identity;
        this.name = name;
    }

    public static String getName(String identity) {
        for (SampleTypeEnum c : SampleTypeEnum.values()) {
            if (c.getIdentity().equals(identity)) {
                return c.name;
            }
        }
        return null;
    }
}
