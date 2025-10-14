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

public enum MeasureTypeEnum {

    PHOTOMETRIC_MEASUREMENT("光度测量"),
    QUANTITATIVE_MEASUREMENT("定量测定"),
    SPECTRAL_SCAN("光谱扫描"),
    TIME_SCAN("时间扫描"),
    TWO_COMPONENT_QUANTITATIVE_ANALYSIS("双组份定量分析");

    private final String description;

    MeasureTypeEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }

    // 根据 description 查找对应的枚举值
    public static MeasureTypeEnum fromDescription(String description) {
        for (MeasureTypeEnum type : MeasureTypeEnum.values()) {
            if (type.getDescription().equals(description)) {
                return type;
            }
        }
        return null;
    }


}
