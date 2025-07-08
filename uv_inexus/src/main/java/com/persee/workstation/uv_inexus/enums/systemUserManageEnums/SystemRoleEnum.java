package com.persee.workstation.uv_inexus.enums.systemUserManageEnums;

import lombok.Getter;

@Getter
public enum SystemRoleEnum {

    DEVELOPER("1","开发人员"),
    PRODUCTION_LINE("2","产线人员"),
    REPAIR_PERSON("3","维修人员"),
    CUSTOMER("4","客户人员");

    private final String sid;

    private final String name;

    SystemRoleEnum(String sid, String name){
        this.sid = sid;
        this.name = name;
    }

}
