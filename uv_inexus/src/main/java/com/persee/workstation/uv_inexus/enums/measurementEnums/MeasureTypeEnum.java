/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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
