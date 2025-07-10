package com.persee.workstation.uv_inexus.constant;

public class PythonPathConstant {
    // 最小二乘接口路径
    public final static String LEAST_SQUARE_FIT = "http://192.168.32.62:8002/least_squares_fitting";
    // 计算浓度或吸收率接口路径
    public final static String CONCENTRATION_OR_ABSORPTION_RATE = "http://192.168.32.60:8000/calculate_concentration_or_abs";
    // 代数运算接口路径
    public final static String ALGEBRAIC_OPERATION = "http://192.168.32.60:8000/algebraic_operations";
    // 寻峰接口路径
    public final static String PEAK_SEARCHING = "http://192.168.32.60:8000/get_peak";
    // 寻谷接口路径
    public final static String VALLEY_SEARCHING = "http://192.168.32.60:8000/get_valley";
    // 谱图的四则运算接口路径
    public final static String SPECTRUM_FOUR_OPERATION = "http://192.168.32.60:8000/spectral_arithmetic_operations";
    // 谱图变换
    public final static String SPECTRUM_TRANSFORMATION = "http://192.168.32.60:8000/spectrum_transform";
    // 消除台阶或者倒刺
    public final static String REMOVE_STEP_OR_SPIKES = "http://192.168.32.60:8000/detect_step_skipe";
    // 微分
    public final static String DIFFERENTIATED = "http://192.168.32.60:8000/differentiated";
    // 平滑
    public final static String SMOOTHED = "http://192.168.32.60:8000/smoothed";

}
