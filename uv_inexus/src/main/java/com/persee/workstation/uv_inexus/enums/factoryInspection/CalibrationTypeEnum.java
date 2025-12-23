package com.persee.workstation.uv_inexus.enums.factoryInspection;

import lombok.Getter;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
//检定校准相关枚举类
@Getter
public enum CalibrationTypeEnum {
    //删除状态
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

    //关联表的物质类型
    MEASUREMENT("measurement", "测量物质"),
    ZERO("zero", "校零物质"),
    //检定校准传入的类型，用来做什么
    SINGLE_ITEM("0", "单项检测"),
    AUTO("1", "自动检测"),
    SET_STANDARD("2", "设置标准物"),

    //任务的执行状态
    NOT_STARTED("0", "未开始"),
    IN_PROGRESS("1", "未完成"),
    COMPLETED("2", "已完成"),

    //检测项类型
    SPECTRAL_SCAN("0", "光谱扫描"),
    TIME_SCAN("1", "时间扫描"),
    PHOTOMETRIC_SCAN("2", "光度扫描"),

    CHARACTERISTIC("characteristic", "特征值"),
    STANDARD("standard","标准值"),
    PROJECT_NAME("projectName", "项目名称"),
    TEST_CATEGORY("testCategory", "检测类别"),
    BLACK_BLOCK("blackBlock", "黑挡块"),
    AIR("air", "空气"),

    CLOSE("close", "关闭"),

    //标准物质的类型
    BUILT_IN("BuiltIn", "内置物"),
    BASE_FIXED_SUBSTANCE("BaseFixedSubstance", "初始固定不可删除物质"),
    EXTERNAL_REFERENCE("ExtRef", "外部参考物"),
//模板类型
    TEMPLATE_BUILT_IN("0", "内置模板"),
    TEMPLATE_USER_DEFINED("1", "用户模板"),

    //任务完成类型  未开始 未完成  已完成
    TASK_NOT_STARTED("0", "未开始"),
    TASK_IN_PROGRESS("1", "未完成"),
    TASK_COMPLETED("2", "已完成"),


    //新建模板或者修改模板
    TEMPLATE_CREATE("0", "从源模板-新建模板"),
    TEMPLATE_UPDATE("1", "修改原模板");

    private final String code;
    private final String description;

    CalibrationTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }


}
