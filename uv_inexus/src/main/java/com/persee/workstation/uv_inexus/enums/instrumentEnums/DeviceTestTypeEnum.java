package com.persee.workstation.uv_inexus.enums.instrumentEnums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum DeviceTestTypeEnum {
    PHOTOMETRIC_MEASUREMENT("1","光度测量"),
    QUANTITATIVE_MEASUREMENT("2", "定量测量"),
    TIME_SCAN("3","时间扫描"),
    SPECTRAL_SCANNING("4","光谱扫描"),
    TWO_COMPONENT_PAINT("5","双组分");

    private final String code;

    private final String name;

    private static final Map<String, String> map = new HashMap<>();

    static {
        for(DeviceTestTypeEnum e : DeviceTestTypeEnum.values()) {
            map.put(e.code,e.name);
        }
    }

    private DeviceTestTypeEnum(String code, String name){
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
