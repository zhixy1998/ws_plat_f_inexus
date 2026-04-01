package com.persee.workstation.uv_inexus.enums.instrumentEnums;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum MappingParamEnum {

    LUMINOSITY("luminosity", "光度模式", Arrays.asList(
            new ParamOption("Abs", "1"),
            new ParamOption("T%", "2"),
            new ParamOption("Es", "3"),
            new ParamOption("Er", "4")
    )),

    LAMP_TYPE("lampType", "灯类型", Arrays.asList(
            new ParamOption("钨灯", "0"),
            new ParamOption("氘灯", "1")
    )),

    GAIN("gain", "增益", Arrays.asList(1, 2, 3, 4)),

    RESPONSE_TIME("responseTime", "响应时间", Arrays.asList("0.5", "1", "2")),

    SCAN_SPEED("scanSpeed", "扫描速度", Arrays.asList(
            new ParamOption("最慢", "slowest"),
            new ParamOption("慢速", "slow"),
            new ParamOption("中速", "medium"),
            new ParamOption("快速", "fast"),
            new ParamOption("极快", "fastest")
    )),

    MIN_WAVELENGTH("minWavelength", "最小波长", "190"),

    MAX_WAVELENGTH("maxWavelength", "最大波长", "1100"),

    SCAN_INTERVAL("scanInterval", "扫描间隔", Arrays.asList("0.1", "0.2", "0.5", "1.0", "2.0", "5.0")),

    BAND_WIDTH("bandWidth", "带宽", generateBandWidthList(0.1, 5.0, 0.1));

    private final String key;           // 参数key
    private final String name;          // 参数中文名
    private final Object defaultValue;  // 参数的默认值/可选值

    MappingParamEnum(String key, String name, Object defaultValue) {
        this.key = key;
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * 生成指定范围的列表
     * @param start 起始值
     * @param end 结束值
     * @param step 步长
     * @return 字符串列表
     */
    private static List<String> generateBandWidthList(double start, double end, double step) {
        List<String> list = new ArrayList<>();
        // 使用BigDecimal避免浮点数精度问题
        BigDecimal current = BigDecimal.valueOf(start);
        BigDecimal endValue = BigDecimal.valueOf(end);
        BigDecimal stepValue = BigDecimal.valueOf(step);

        while (current.compareTo(endValue) <= 0) {
            // 保留一位小数
            list.add(current.setScale(1, RoundingMode.HALF_UP).toString());
            current = current.add(stepValue);
        }
        return list;
    }

    /**
     * 获取参数类型
     */
    public ParamValueType getValueType() {
        if (defaultValue instanceof List) {
            List<?> list = (List<?>) defaultValue;
            if (!list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof ParamOption) {
                    return ParamValueType.LIST_OBJECT;
                } else if (first instanceof Integer) {
                    return ParamValueType.LIST_INTEGER;
                } else if (first instanceof String) {
                    return ParamValueType.LIST_STRING;
                }
            }
            return ParamValueType.LIST;
        } else if (defaultValue instanceof String) {
            return ParamValueType.STRING;
        }
        return ParamValueType.UNKNOWN;
    }

    /**
     * 根据key获取枚举
     */
    public static MappingParamEnum fromKey(String key) {
        for (MappingParamEnum param : values()) {
            if (param.getKey().equals(key)) {
                return param;
            }
        }
        return null;
    }

    /**
     * 参数值类型枚举
     */
    public enum ParamValueType {
        STRING("字符串"),
        LIST_STRING("字符串列表"),
        LIST_INTEGER("整数列表"),
        LIST_OBJECT("对象列表"),
        LIST("列表"),
        UNKNOWN("未知");

        private final String description;

        ParamValueType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 参数选项内部类
     */
    public static class ParamOption {
        private String paramName;
        private String paramValue;

        public ParamOption() {
        }

        public ParamOption(String paramName, String paramValue) {
            this.paramName = paramName;
            this.paramValue = paramValue;
        }

        public String getParamName() {
            return paramName;
        }

        public void setParamName(String paramName) {
            this.paramName = paramName;
        }

        public String getParamValue() {
            return paramValue;
        }

        public void setParamValue(String paramValue) {
            this.paramValue = paramValue;
        }
    }
}