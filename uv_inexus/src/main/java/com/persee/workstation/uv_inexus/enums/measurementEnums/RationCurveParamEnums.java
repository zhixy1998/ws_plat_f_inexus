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
public enum RationCurveParamEnums {

    no_differential("no_differential","不微分"),
    one_differential("one_differential","一次微分"),
    two_differential("two_differential","二次微分"),
    three_differential("three_differential","三次微分"),
    four_differential("four_differential","四次微分"),

    one("1", "一次曲线"),
    two("2", "二次曲线"),
    three("3", "三次曲线"),
    four("4", "四次曲线"),
    typeAbs("f_Abs","C=f(Abs)"),
    typeCurve("f_C", "Abs=f(C)"),

    direct_coefficient("direct","直接系数法"),
    standard_curve("standard","标准曲线法"),

    //单池重复
    single_pool_repeat("单池重复","单池重复"),
    //不重复
    no_repeat("不重复","不重复"),

    insert_zero("insert_zero","插入零点"),
    force_zero("force_zero","强制过零点");


    private final String identity;

    private final String name;


    RationCurveParamEnums(String identity, String name){
        this.identity = identity;
        this.name = name;
    }

    public static String getName(String identity) {
        for (RationCurveParamEnums c : RationCurveParamEnums.values()) {
            if (c.getIdentity().equals(identity)) {
                return c.name;
            }
        }
        return null;
    }
}
