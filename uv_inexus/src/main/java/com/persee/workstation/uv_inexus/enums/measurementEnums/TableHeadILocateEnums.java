package com.persee.workstation.uv_inexus.enums.measurementEnums;

import lombok.Getter;

@Getter
public enum TableHeadILocateEnums {
    system("system", "系统预置表头"),
    sample("sample", "样品表头"),
    data("data","测量页用户新增表头"),
    measure("measure", "后端添加测量表头"),
    analyze("analyze", "方法来的分析表头");

    private final String identity;

    private final String name;


    TableHeadILocateEnums(String identity, String name){
        this.identity = identity;
        this.name = name;
    }

    public static String getName(String identity) {
        for (TableHeadILocateEnums c : TableHeadILocateEnums.values()) {
            if (c.getIdentity().equals(identity)) {
                return c.name;
            }
        }
        return null;
    }
}
