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

package com.persee.workstation.uv_inexus.enums.systemManageEnums;

import lombok.Getter;

@Getter
public enum AutoTimeEnum {
    unAutoSave("不自动保存",0),
    fiveMinutes("5分钟",1),
    fifteen("15分钟",2),
    halfOneHour("半小时",3),
    oneHour("1小时",4);

    private final String timeEnum;
    private final Integer value;

    AutoTimeEnum(String timeEnum, Integer value) {
        this.timeEnum = timeEnum;
        this.value = value;
    }
    public static String getByTimeEnum(Integer value) {
        for (AutoTimeEnum autoTimeEnum : AutoTimeEnum.values()) {
            if (autoTimeEnum.getValue().equals(value)) {
                return autoTimeEnum.getTimeEnum();
            }
        }
        return null;
    }

}
