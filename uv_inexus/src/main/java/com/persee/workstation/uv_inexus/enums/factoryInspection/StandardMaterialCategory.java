package com.persee.workstation.uv_inexus.enums.factoryInspection;

/**
 * Standard Material Category Enum
 * 标准物种类枚举
 */
public enum StandardMaterialCategory {

    /**
     * External reference material
     * 外部参考物
     */
    EXTERNAL_REFERENCE("外部参考物", "ExtRef"),

    /**
     * Non-standard external reference material
     * 非标准外部参考物
     */
    NON_STANDARD_EXTERNAL_REFERENCE("非标准外部参考物", "NonStdExtRef");

    private final String description;
    private final String code;

    StandardMaterialCategory(String chineseName, String englishName) {
        this.description = chineseName;
        this.code = englishName;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }
}