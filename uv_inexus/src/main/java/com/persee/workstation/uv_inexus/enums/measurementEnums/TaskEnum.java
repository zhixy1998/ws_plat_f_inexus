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
public enum TaskEnum {
    finished("1","已完成"),
    un_finished("0","未完成");

    private final String status;

    private final String message;

    TaskEnum(String status, String message) {
        this.status = status;
        this.message = message;
    }
    public static TaskEnum getTaskEnum(String status) {
        for (TaskEnum taskEnum : TaskEnum.values()) {
            if (taskEnum.status.equals(status)) {
                return taskEnum;
            }
        }
        return null;
    }
    public static String getTaskStr(String status) {
        return getTaskEnum(status).getMessage();
    }
}
