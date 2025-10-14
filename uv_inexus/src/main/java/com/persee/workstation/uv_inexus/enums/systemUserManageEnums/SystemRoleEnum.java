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

package com.persee.workstation.uv_inexus.enums.systemUserManageEnums;

import lombok.Getter;

@Getter
public enum SystemRoleEnum {

    DEVELOPER("1","开发人员"),
    PRODUCTION_LINE("2","产线人员"),
    REPAIR_PERSON("3","维修人员"),
    CUSTOMER("4","客户人员");

    private final String sid;

    private final String name;

    SystemRoleEnum(String sid, String name){
        this.sid = sid;
        this.name = name;
    }

}
