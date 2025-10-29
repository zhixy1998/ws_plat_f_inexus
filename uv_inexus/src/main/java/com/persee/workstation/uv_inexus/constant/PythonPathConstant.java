/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.constant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class PythonPathConstant {

    @Value("${python-code.baseUrl}")
    public static String httpUrl;


    // 最小二乘接口路径
    public static String LEAST_SQUARE_FIT;
    // 计算浓度或吸收率接口路径
    public static String CONCENTRATION_OR_ABSORPTION_RATE;
    // 代数运算接口路径
    public static String ALGEBRAIC_OPERATION;
    // 寻峰接口路径
    public static String PEAK_SEARCHING;
    // 寻谷接口路径
    public static String VALLEY_SEARCHING;
    // 寻峰寻谷接口路径
    public static String PEAK_SEARCHING_AND_VALLEY_SEARCHING;
    // 光谱四则运算接口路径
    public static String SPECTRUM_FOUR_OPERATION;
    // 光谱变换接口路径
    public static String SPECTRUM_TRANSFORMATION;
    // 消除台阶或者倒刺
    public static String REMOVE_STEP_OR_SPIKES;
    // 微分
    public static String DIFFERENTIATED;
    // 平滑
    public static String SMOOTHED;

    @PostConstruct
    public void init() {
        // 在构造器后初始化静态字段
        LEAST_SQUARE_FIT = httpUrl + "least_squares_fitting";
        CONCENTRATION_OR_ABSORPTION_RATE = httpUrl + "calculate_concentration_or_abs";
        ALGEBRAIC_OPERATION = httpUrl + "algebraic_operations";
        PEAK_SEARCHING = httpUrl + "get_peak";
        VALLEY_SEARCHING = httpUrl + "get_valley";
        PEAK_SEARCHING_AND_VALLEY_SEARCHING = httpUrl + "get_peaks_and_valleys";
        SPECTRUM_FOUR_OPERATION = httpUrl + "spectral_arithmetic_operations";
        SPECTRUM_TRANSFORMATION = httpUrl + "spectrum_transform";
        REMOVE_STEP_OR_SPIKES = httpUrl + "detect_step_skipe";
        DIFFERENTIATED = httpUrl + "differentiated";
        SMOOTHED = httpUrl + "smoothed";
    }

}
