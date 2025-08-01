package com.persee.workstation.uv_inexus.enums.measurementEnums;

import lombok.Getter;

@Getter
public enum RationDataIdentityEnums {
    result("result", "结果"),
    middle("middle", "中值"),
    sd("sd", "标准差"),
    rsd("rsd", "RSD");

    private final String identity;

    private final String name;


    RationDataIdentityEnums(String identity, String name){
        this.identity = identity;
        this.name = name;
    }

    public static String getName(String identity) {
        for (RationDataIdentityEnums c : RationDataIdentityEnums.values()) {
            if (c.getIdentity().equals(identity)) {
                return c.name;
            }
        }
        return null;
    }


}
