/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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
