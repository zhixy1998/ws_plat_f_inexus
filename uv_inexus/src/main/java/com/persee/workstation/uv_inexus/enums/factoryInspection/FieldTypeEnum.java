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
 * 表单字段类型枚举
 */

public enum FieldTypeEnum {
    /**
     * 输入框类型
     */
    INPUT("input", "输入框"),

    /**
     * 文本域类型
     */
    TEXTAREA("textarea", "文本域"),

    /**
     * 下拉选择框类型
     */
    SELECT("select", "下拉框"),

    /**
     * 复选框类型
     */
    CHECKBOX("checkbox", "复选框"),

    /**
     * 单选框类型
     */
    RADIO("radio", "单选框"),

    /**
     * 日期选择器类型
     */
    DATE("date", "日期选择器"),

    /**
     * 时间选择器类型
     */
    TIME("time", "时间选择器"),

    /**
     * 数字输入框类型
     */
    NUMBER("number", "数字输入框"),

    /**
     * 文件上传类型
     */
    FILE("file", "文件上传");

    private final String code;
    private final String description;

    FieldTypeEnum(String code, String description) {
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
     * 根据code获取枚举值
     * @param code 字段类型编码
     * @return 对应的枚举值
     */
    public static FieldTypeEnum fromCode(String code) {
        for (FieldTypeEnum type : FieldTypeEnum.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的字段类型: " + code);
    }
}
