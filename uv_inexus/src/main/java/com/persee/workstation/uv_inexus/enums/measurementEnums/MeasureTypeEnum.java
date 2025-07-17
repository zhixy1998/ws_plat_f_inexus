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
        throw new IllegalArgumentException("未知的测量类型: " + description);
    }


}
