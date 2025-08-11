package com.persee.workstation.uv_inexus.enums.measurementEnums;

import lombok.Getter;

@Getter
public enum SampleTableEnum {

    sampleId("sampleId", "样品ID"),
    sampleNumber("sampleNumber", "样品编号"),
    sampleType("sampleType", "样品类型"),
    sampleKind("sampleKind", "样品类别"),
    sampleName("sampleName", "样品名称"),
    dilutionRatio("dilutionRatio", "稀释倍数"),
    amount("amount", "取样量"),
    normalConcentration("normalConcentration", "标准浓度"),
    fitConcentration("fitConcentration", "拟合浓度"),
    sampleLocation("sampleLocation", "样品位置"),
    Abs("Abs", "Abs"),
    sumAbs("sumAbs", "▲Abs"),
    remark("remark", "备注"),
    samplingTime("samplingTime", "采样时间");

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
