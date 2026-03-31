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
    HISTORY_WAVEFORM("3", "历史波形图"),
    RESUME("4", "用户动作，标准物放置后点击"),

    //任务的执行状态
    NOT_STARTED("0", "未开始"),
    IN_PROGRESS("1", "未完成"),
    COMPLETED("2", "已完成"),

    //检测项类型
    SPECTRAL_SCAN("0", "光谱扫描"),
    TIME_SCAN("1", "时间扫描"),
    PHOTOMETRIC_SCAN("2", "光度扫描"),

    //是否为光谱扫描

    SPECTRAL_SCAN_TYPE("spectralScan", "是否为光谱扫描"),

    //扫描速度
    SCAN_SPEED("scanSpeed", "扫描速度"),

    //时间间隔
    TIME_INTERVAL("timeInterval", "时间间隔"),
    //起始波长
    START_WAVELENGTH("startWavelength", "起始波长"),
    //终止波长
    END_WAVELENGTH("endWavelength", "终止波长"),
    //扫描间隔
    SCAN_INTERVAL("scanInterval", "扫描间隔"),
    //增益
    GAIN("gain", "增益"),
    //是
    YES("1", "是"),
    NO("0", "否"),
    //光谱带宽
    SPECTRAL_BANDWIDTH("spectralBandwidth", "光谱带宽"),
    //灯状态
    LAMP_STATUS("lampStatus", "灯状态"),
    CHARACTERISTIC("characteristic", "特征值"),
    STANDARD("standard","标准值"),
    PROJECT_NAME("projectName", "项目名称"),
    TEST_CATEGORY("testCategory", "检测类别"),
    BLACK_BLOCK("blackBlock", "黑挡块"),
    AIR("air", "空气"),
    MEASUREMENT_VALUE("measurementValue", "测量值"),
    //扫描个数
    SCAN_COUNT("scanCount", "扫描个数"),
    //响应时间
    RESPONSE_TIME("responseTime", "响应时间"),

    MEASUREMENT_STATUS_NOT_PERFORMED("0", "未检测"),
    MEASUREMENT_STATUS_SUCCESS("1", "检测成功"),
    MEASUREMENT_STATUS_FAILED("2", "检测失败"),


    CHECK_RESULT_NOT_EVALUATED("0", "未检测"),
    CHECK_RESULT_PASS("1", "合格"),
    CHECK_RESULT_FAIL("2", "不合格"),

    //检测项信息
    CHECK_ITEM_INFO("measurementItemType", "检测项信息"),
    // 光度模式
    LUMINOSITY_MODE("luminosityMode", "光度模式"),
    CLOSE("close", "关闭"),

    //标准物质的类型
    BUILT_IN("BuiltIn", "内置物"),
    BASE_FIXED_SUBSTANCE("Fixed", "初始固定不可删除物质"),
    EXTERNAL_REFERENCE("ExtRef", "外部参考物"),
//模板类型
    TEMPLATE_BUILT_IN("0", "内置模板"),
    TEMPLATE_USER_DEFINED("1", "用户模板"),

    //任务完成类型  未开始 未完成  已完成
    TASK_NOT_STARTED("0", "未开始"),
    TASK_IN_PROGRESS("1", "未完成"),
    TASK_COMPLETED("2", "已完成"),

    //任务分类 默认任务  用户任务
    TEMPLATE_BUILT_IN_TASK("0", "默认任务"),
    TEMPLATE_USER_DEFINED_TASK("1", "用户任务"),

    //重复次数
    REPEAT_COUNT("repeatCount", "重复次数"),
    //默认任务
    DEFAULT_TASK("default", "默认任务"),

    //开箱检验任务
    OPEN_BOX_INSPECTION("installation_acceptance", "开箱检验"),

    //出厂检验任务
    FACTORY_INSPECTION("factory_inspection", "出厂检验"),
    //分析方法
    METHOD_TYPE("methodType", "分析方法"),


    // 校准检定工作表名称枚举
    WAVELENGTH_CALIBRATION("wavelength_accuracy_repeatability", "波长示值误差与重复性校验"),
    TRANSMITTANCE_CALIBRATION("transmittance_accuracy_repeatability", "透射比示值误差与重复性校验"),
    STRAY_LIGHT_CALIBRATION("stray_light", "杂散光校验"),
    BASELINE_STRAIGHTNESS_INSPECTION("baseline_straightness", "基线平直度检验"),
    BASELINE_DARK_NOISE_INSPECTION("baseline_dark_noise", "基线暗噪声检验"),
    SPECTRAL_BANDWIDTH_CALIBRATION("spectral_bandwidth", "光谱带宽与仪器最小光谱带宽"),
    NOISE_INSPECTION("noise", "噪声检验"),
    WAVELENGTH_EDGE_NOISE("wavelength_edge_noise", "波长边缘噪声"),
    DRIFT_INSPECTION("drift", "漂移检验"),


    //波长示值误差
    WAVELENGTH_ACCURACY("wavelength_accuracy", "波长示值误差"),
    //波长重复性
    WAVELENGTH_REPEATABILITY("wavelength_repeatability", "波长重复性"),
    //透射比示值误差
    TRANSMITTANCE_ACCURACY("transmittance_accuracy", "透射比示值误差"),
    //透射比重复性
    TRANSMITTANCE_REPEATABILITY("transmittance_repeatability", "透射比重复性"),
    //指标要求
    INDICATOR_REQUIREMENT("indicator_requirement", "指标要求"),
    //误差范围
    ERROR_RANGE("error_range", "误差范围"),
    //垂直距离范围
    VERTICAL_DISTANCE_RANGE("vertical_distance_range", "垂直距离范围"),






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
