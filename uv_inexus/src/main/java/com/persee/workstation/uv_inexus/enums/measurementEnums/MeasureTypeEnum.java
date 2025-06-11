package com.persee.workstation.uv_inexus.enums.measurementEnums;

import lombok.Getter;

/**
 * 测量类型枚举类
 * 对应规则：
 * 0 - 光度测量
 * 1 - 定量测量
 * 2 - 光谱扫描
 * 3 - 时间扫描
 * 4 - 双组份定量分析
 */
@Getter
public enum MeasureTypeEnum {

    /** 光度测量 */
    PHOTOMETRIC_MEASUREMENT("0", "光度测量"),

    /** 定量测量 */
    QUANTITATIVE_MEASUREMENT("1", "定量测量"),

    /** 光谱扫描 */
    SPECTRAL_SCAN("2", "光谱扫描"),

    /** 时间扫描 */
    TIME_SCAN("3", "时间扫描"),

    /** 双组份定量分析 */
    TWO_COMPONENT_QUANTITATIVE_ANALYSIS("4", "双组份定量分析");

    /** 枚举值（对应数据库存储的数值） */
    private final String value;

    /** 枚举描述（业务含义） */
    private final String description;

    /**
     * 构造方法
     * @param value 数值标识
     * @param description 业务描述
     */
    MeasureTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    // --------------------------- 常用工具方法 ---------------------------

    /**
     * 通过数值获取枚举实例
     * @param value 数值标识
     * @return 对应的枚举对象，未找到返回 null
     */
    public static MeasureTypeEnum of(String value) {
        for (MeasureTypeEnum enumItem : MeasureTypeEnum.values()) {
            if (enumItem.value == value) {
                return enumItem;
            }
        }
        return null;
    }

    /**
     * 获取数值标识
     * @return int 枚举值
     */
    public String getValue() {
        return value;
    }

    /**
     * 获取业务描述
     * @return String 描述信息
     */
    public String getDescription() {
        return description;
    }
}