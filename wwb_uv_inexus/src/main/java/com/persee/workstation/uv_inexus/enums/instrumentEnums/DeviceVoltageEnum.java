package com.persee.workstation.uv_inexus.enums.instrumentEnums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum DeviceVoltageEnum {

    VOLTAGE_POSITIVE_5("21","电压 +5V"),
    VOLTAGE_POSITIVE_12("22","电压 +12V"),
    VOLTAGE_POSITIVE_15("23","电压 +15V"),

    VOLTAGE_POSITIVE_24("24","电压 +24V"),
    VOLTAGE_NEGATIVE_15("25","电压 -15V"),
    TUNGSTEN_LAMP_VOLTAGE("26","钨灯电压"),
    DEUTERIUM_LAMP_VOLTAGE("27","氘灯电压");


    private final String code;

    private final String name;

    private static final Map<String, String> map = new HashMap<>();

    static {
        for(DeviceVoltageEnum e : DeviceVoltageEnum.values()) {
            map.put(e.code,e.name);
        }
    }

    private DeviceVoltageEnum(String code, String name){
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
