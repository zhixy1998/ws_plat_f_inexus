package com.persee.workstation.uv_inexus.enums.instrumentEnums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 初始化信息枚举类
 */
@Getter
public enum DeviceInitInfoEnum {

    WAVELENGTH_LIST("1","读取波长列表"),
    INITIALIZATION_STATUS("2", "读取初始化状态"),
    DEUTERIUM_LAMP_MAXIMUM_ENERGY("3","氘灯最高能量值"),
    TUNGSTEN_LAMP_MAXIMUM_ENERGY_VALUE("4","钨灯最高能量值"),
    DEUTERIUM_LAMP_WORKING_TIME("5","氘灯已工作时长"),
    TUNGSTEN_LAMP_WORKING_TIME("6","钨灯已工作时长");

    private final String code;

    private final String name;

    private static final Map<String, String> map = new HashMap<>();

    static {
        for(DeviceInitInfoEnum e : DeviceInitInfoEnum.values()) {
            map.put(e.code,e.name);
        }
    }

    private DeviceInitInfoEnum(String code, String name){
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
