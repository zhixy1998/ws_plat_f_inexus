package com.persee.workstation.uv_inexus.enums.measurementEnums;

import lombok.Getter;

@Getter
public enum EnergyLampEnums {

    Tungsten("0", "钨灯"),
    Deuterium("1", "氘灯"),
    Mercury("2", "汞灯");

    private final String identity;

    private final String name;


    EnergyLampEnums(String identity, String name){
        this.identity = identity;
        this.name = name;
    }

    public static String getName(String identity) {
        for (EnergyLampEnums c : EnergyLampEnums.values()) {
            if (c.getIdentity().equals(identity)) {
                return c.name;
            }
        }
        return null;
    }
}
