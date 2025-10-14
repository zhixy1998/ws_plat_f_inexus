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

import java.util.HashMap;
import java.util.Map;

@Getter
public enum SystemManageEnum {
    methodPanel("method","方法面板"),
    projectPanel("project","项目面板"),
    taskPanel("task","任务面板");


    private final String name;

    private final String description;

    SystemManageEnum(String name, String description){
        this.description = description;
        this.name = name;
    }
    private static final Map<String, String> map = new HashMap<>();

    static {
        for(SystemManageEnum e : SystemManageEnum.values()) {
            map.put(e.name,e.description);
        }
    }


    public static String getName(String name) {
        if(map.containsKey(name)) {
            return map.get(name);
        }
        return "";
    }
}
