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
 * Standard Material Category Enum
 * 标准物种类枚举
 */
public enum StandardMaterialCategory {

    /**
     * External reference material
     * 外部参考物
     */
    EXTERNAL_REFERENCE("外部参考物", "ExtRef"),
    MEASUREMENT("测量物质", "measurement"),
    ZERO("校零物质", "zero"),

    /**
     * Non-standard external reference material
     * 非标准外部参考物
     */
    NON_STANDARD_EXTERNAL_REFERENCE("非标准外部参考物", "NonStdExtRef");

    private final String description;
    private final String code;

    StandardMaterialCategory(String description, String code) {
        this.description = description;
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }
}