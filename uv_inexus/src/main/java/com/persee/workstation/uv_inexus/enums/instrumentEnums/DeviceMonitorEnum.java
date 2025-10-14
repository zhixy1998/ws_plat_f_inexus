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

package com.persee.workstation.uv_inexus.enums.instrumentEnums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 监控信息条目枚举类
 */
@Getter
public enum DeviceMonitorEnum {
    DEVICE_PARAMETER("1","仪器参数"),
    DEVICE_USAGE_TIME("2", "累计工作时长"),
    DEVICE_VOLTAGE("3","电流电压信息");

    private final String code;

    private final String name;

    private static final Map<String, String> map = new HashMap<>();

    static {
        for(DeviceMonitorEnum e : DeviceMonitorEnum.values()) {
            map.put(e.code,e.name);
        }
    }

    private DeviceMonitorEnum(String code, String name){
        this.code = code;
        this.name = name;
    }

    public static String getName(String code) {
        if(map.containsKey(code)) {
            return map.get(code);
        }
        throw new IllegalArgumentException("Invalid key: " + code);
    }

}
