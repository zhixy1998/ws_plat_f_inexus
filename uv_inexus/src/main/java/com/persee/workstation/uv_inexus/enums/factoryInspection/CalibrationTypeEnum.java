package com.persee.workstation.uv_inexus.enums.factoryInspection;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
//检定校准相关枚举类
public enum CalibrationTypeEnum {

    NOT_DELETED("0", "未删除"),
    DELETED("1", "已删除"),
    INPUT("input", "输入框"),
    TEXTAREA("textarea", "文本域"),
    SELECT("select", "下拉框"),
    CHECKBOX("checkbox", "复选框"),
    RADIO("radio", "单选框"),
    DATE("date", "日期选择器"),
    TIME("time", "时间选择器"),
    NUMBER("number", "数字输入框"),
    FILE("file", "文件上传"),


    MEASUREMENT("measurement", "测量物质"),
    ZERO("zero", "校零物质"),
//测试类型
    SINGLE_ITEM("0", "单项检测"),
    AUTO("1", "自动检测"),


    NOT_STARTED("0","未开始"),
    IN_PROGRESS("1","未完成"),
    COMPLETED("2","已完成"),

    BUILT_IN("BuiltIn", "内置物"),
    EXTERNAL_REFERENCE("ExtRef", "外部参考物"),
    NON_STANDARD_EXTERNAL_REFERENCE("NonStdExtRef", "非标准外部参考物");

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    private final String code;
    private final String description;

    CalibrationTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }


}
