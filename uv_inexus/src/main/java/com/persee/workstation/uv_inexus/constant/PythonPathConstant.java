package com.persee.workstation.uv_inexus.constant;

public class PythonPathConstant {
    private static final String httpUrl= "http://192.168.61.82:8002/";
    // 最小二乘接口路径
    public final static String LEAST_SQUARE_FIT = httpUrl+"least_squares_fitting";
    // 计算浓度或吸收率接口路径
    public final static String CONCENTRATION_OR_ABSORPTION_RATE = httpUrl+"calculate_concentration_or_abs";
    // 代数运算接口路径
    public final static String ALGEBRAIC_OPERATION = httpUrl+"algebraic_operations";
    // 寻峰接口路径
    public final static String PEAK_SEARCHING = httpUrl+"get_peak";
    // 寻谷接口路径
    public final static String VALLEY_SEARCHING = httpUrl+"get_valley";
    // 寻峰寻谷接口路径
    public final static String PEAK_SEARCHING_AND_VALLEY_SEARCHING = httpUrl+"get_peaks_and_valleys";
    // 谱图的四则运算接口路径
    public final static String SPECTRUM_FOUR_OPERATION = httpUrl+"spectral_arithmetic_operations";
    // 谱图变换
    public final static String SPECTRUM_TRANSFORMATION = httpUrl+"spectrum_transform";
    // 消除台阶或者倒刺
    public final static String REMOVE_STEP_OR_SPIKES = httpUrl+"detect_step_skipe";
    // 微分
    public final static String DIFFERENTIATED = httpUrl+"differentiated";
    // 平滑
    public final static String SMOOTHED = httpUrl+"smoothed";

}
