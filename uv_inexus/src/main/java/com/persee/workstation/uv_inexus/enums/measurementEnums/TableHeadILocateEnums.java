/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.enums.measurementEnums;

import lombok.Getter;

@Getter
public enum TableHeadILocateEnums {
    system("system", "系统预置表头"),
    sample("sample", "样品表头"),
    location("location", "位置"),
    data("data","测量页用户新增表头"),
    measure("measure", "后端添加测量表头"),
    other("other", "其他表头,如采样时间，备注等"),
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
