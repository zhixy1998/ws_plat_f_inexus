package com.persee.workstation.uv_inexus.enums.measurementEnums;

public enum MeasureTypeEnum {

    PHOTOMETRIC_MEASUREMENT("光度测量"),
    QUANTITATIVE_MEASUREMENT("定量测量"),
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
}
