package com.persee.workstation.uv_inexus.enums.instrumentEnums;

import com.persee.workstation.uv_inexus.vo.concise.common.DeviceConfParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum MappingParamEnum {

    LUMINOSITY("luminosity", "光度模式", Arrays.asList(
            new DeviceConfParam("Abs", "1"),
            new DeviceConfParam("T%", "2"),
            new DeviceConfParam("Es", "3"),
            new DeviceConfParam("Er", "4")
    )),

    LAMP_TYPE("lampType", "灯类型", Arrays.asList(
            new DeviceConfParam("钨灯", "0"),
            new DeviceConfParam("氘灯", "1")
//            new DeviceConfParam("汞灯", "2")
    )),

    GAIN("gain", "增益", Arrays.asList(1, 2, 3, 4, 5, 6 ,7)),

    RESPONSE_TIME("responseTime", "响应时间", Arrays.asList("0.5", "1", "2")),

    SCAN_SPEED("scanSpeed", "扫描速度", Arrays.asList(
            new DeviceConfParam("最慢", "slowest"),
            new DeviceConfParam("慢速", "slow"),
            new DeviceConfParam("中速", "medium"),
            new DeviceConfParam("快速", "fast"),
            new DeviceConfParam("极快", "fastest")
    )),

    MIN_WAVELENGTH("minWavelength", "最小波长", "185"),

    MAX_WAVELENGTH("maxWavelength", "最大波长", "1100"),

    MIN_LAMP_WAVELENGTH("minLampWavelength", "最小灯波长", "330"),

    MAX_LAMP_WAVELENGTH("maxLampWavelength", "最大灯波长", "390"),

    SCAN_INTERVAL("scanInterval", "扫描间隔", Arrays.asList("0.04","0.1", "0.2", "0.5", "1.0", "2.0", "5.0")),

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
                if (first instanceof DeviceConfParam) {
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
     * 根据扫描速度的paramValue获取对应的DeviceConfParam对象
     * @param paramValue 速度值（如 "slowest", "slow", "medium", "fast", "fastest"）
     * @return 匹配的DeviceConfParam对象，未找到返回null
     */
    public DeviceConfParam getScanSpeedByValue(String paramValue) {
        List<?> list = (List<?>) SCAN_SPEED.defaultValue;
        for (Object item : list) {
            DeviceConfParam param = (DeviceConfParam) item;
            if (param.getParamValue().equals(paramValue)) {
                return param;
            }
        }
        return null;
    }

    /**
     * 根据扫描速度的paramName获取对应的DeviceConfParam对象
     * @param paramName 速度名称（如 "最慢", "慢速", "中速", "快速", "极快"）
     * @return 匹配的DeviceConfParam对象，未找到返回null
     */
    public DeviceConfParam getScanSpeedByName(String paramName) {
        List<?> list = (List<?>) SCAN_SPEED.defaultValue;
        for (Object item : list) {
            DeviceConfParam param = (DeviceConfParam) item;
            if (param.getParamName().equals(paramName)) {
                return param;
            }
        }
        return null;
    }
}