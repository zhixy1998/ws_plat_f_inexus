package com.persee.workstation.uv_inexus.enums.factoryInspection;

/**
 * 标准物质类型枚举
 */
public enum StandardMaterialType {
    /**
     * 内置物
     */
    BUILT_IN("内置物", "BuiltIn"),

    /**
     * 外部参考物
     */
    EXTERNAL_REFERENCE("外部参考物", "ExtRef");

    private final String chineseName;
    private final String code;

    StandardMaterialType(String chineseName, String code) {
        this.chineseName = chineseName;
        this.code = code;
    }

    public String getChineseName() {
        return chineseName;
    }

    public String getCode() {
        return code;
    }
}
