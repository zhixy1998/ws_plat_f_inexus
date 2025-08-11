package com.persee.workstation.uv_inexus.enums.measurementEnums;

import lombok.Getter;

@Getter
public enum RepeatConfigEnum {
    repeat_single("0", "单池重复后再移动"),
    repeat_inTurn("1", "依次移动并重复");

    private final String identity;

    private final String name;


    RepeatConfigEnum(String identity, String name){
        this.identity = identity;
        this.name = name;
    }

    public static String getName(String identity) {
        for (RepeatConfigEnum c : RepeatConfigEnum.values()) {
            if (c.getIdentity().equals(identity)) {
                return c.name;
            }
        }
        return null;
    }
}
