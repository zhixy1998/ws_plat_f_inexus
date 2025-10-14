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
public enum SampleTableEnum {

    SAMPLE_ID("sampleId", "样品ID"),
    SAMPLE_NUMBER("sampleNumber", "样品编号"),
    SAMPLE_TYPE("sampleType", "样品类型"),
    SAMPLE_KIND("sampleKind", "样品类别"),
    SAMPLE_NAME("sampleName", "样品名称"),
    DILUTION_RATIO("dilutionRatio", "稀释倍数"),
    AMOUNT("amount", "取样量"),
    NORMAL_CONCENTRATION("normalConcentration", "标准浓度"),
    FIT_CONCENTRATION("fitConcentration", "拟合浓度"),
    SAMPLE_LOCATION("sampleLocation", "样品位置"),
    Abs("Abs", "Abs"),
    SUM_Abs("sumAbs", "▲Abs"),
    REMARK("remark", "备注"),
    SAMPLING_TIME("samplingTime", "采样时间"),
    ID("id","编号"),
    WAVELENGTH("waveLength", "波长"),
    LUMINOSITY("luminosity", "光度"),
    TIME("time", "时间"),
    PEAKVALLEY("peakValley", "时间");



    private final String identity;

    private final String name;


    SampleTableEnum(String identity, String name){
        this.identity = identity;
        this.name = name;
    }

    public static String getName(String identity) {
        for (SampleTableEnum c : SampleTableEnum.values()) {
            if (c.getIdentity().equals(identity)) {
                return c.name;
            }
        }
        return null;
    }
}
