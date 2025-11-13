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