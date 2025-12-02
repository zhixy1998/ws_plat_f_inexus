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
 * Delete Status Enum
 * 删除状态枚举
 */
public enum DeleteStatusEnum {
    /**
     * Not deleted
     * 未删除
     */
    NOT_DELETED("0", "未删除"),

    /**
     * Deleted
     * 已删除
     */
    DELETED("1", "已删除");

    private final String code;
    private final String description;

    DeleteStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get enum by code
     * @param code status code
     * @return corresponding enum value
     */
    public static DeleteStatusEnum fromCode(String code) {
        for (DeleteStatusEnum status : DeleteStatusEnum.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown delete status code: " + code);
    }
}