package com.persee.workstation.uv_inexus.enums.measurementEnums;

import lombok.Getter;

@Getter
public enum WaveLengthEnum {

    abs("1", "Abs"),
    tPercent("2", "T%"),
    es("3", "Es"),
    er("4", "Er");

    private final String code;

    private final String name;


    WaveLengthEnum(String code, String name){
        this.code = code;
        this.name = name;
    }

    public static String getName(String code) {
        for (WaveLengthEnum c : WaveLengthEnum.values()) {
            if (c.getCode().equals(code)) {
                return c.name;
            }
        }
        return null;
    }
}
