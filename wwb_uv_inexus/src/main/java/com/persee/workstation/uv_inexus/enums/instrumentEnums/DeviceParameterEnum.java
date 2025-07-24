package com.persee.workstation.uv_inexus.enums.instrumentEnums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum DeviceParameterEnum {

    CURRENT_SPECTRAL_BANDWIDTH("01","当前光谱带宽"),
    CURRENT_WAVELENGTH("02","当前波长"),
    CURRENT_COLOR_FILTER("03","当前滤色片"),
    SAMPLE_FILM_VALUE_1("04","1%样品衰减片值"),
    SAMPLE_FILM_VALUE_10("05","10%样品衰减片值"),
    REFERENCE_FILM_VALUE_1("06","1%参比衰减片值"),
    REFERENCE_FILM_VALUE_10("07","10%参比衰减片值");


    private final String code;

    private final String name;

    private static final Map<String, String> map = new HashMap<>();

    static {
        for(DeviceParameterEnum e : DeviceParameterEnum.values()) {
            map.put(e.code,e.name);
        }
    }

    private DeviceParameterEnum(String code, String name){
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
