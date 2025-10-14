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
 * 初始化信息枚举类
 */
@Getter
public enum DeviceInitInfoEnum {

    WAVELENGTH_LIST(1,"附件初始化"),
    COLOR_FILTER(2, "滤色片电机初始化"),
    INIT_SLIT_MOTOR(3,"狭缝电机初始化"),
    SCANNING_MOTOR(4,"扫描电机初始化"),
    LIGHT_SOURCE_MOTOR(5,"光源电机初始化"),
    TUNGSTEN_LAMP_ENERGY(6,"钨灯能量检查"),
    DEUTERIUM_LAMP_ENERGY(7,"氘灯能量检查"),
    WAVELENGTH(8,"波长检查");


    private final Integer code;

    private final String name;

    private static final Map<Integer, String> map = new HashMap<>();

    static {
        for(DeviceInitInfoEnum e : DeviceInitInfoEnum.values()) {
            map.put(e.code,e.name);
        }
    }

    private DeviceInitInfoEnum(Integer code, String name){
        this.code = code;
        this.name = name;
    }


    public static String getName(Integer code) {
        if(map.containsKey(code)) {
            return map.get(code);
        }
        throw new IllegalArgumentException("Invalid key: " + code);
    }

    public static Integer getLength(){
        return map.size();
    }
}
