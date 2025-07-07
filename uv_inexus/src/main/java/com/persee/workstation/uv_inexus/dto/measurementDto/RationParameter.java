package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 定量测量参数
 */
@Data
public class RationParameter extends MeasurePublicParameters{
    // 波长值
    private List<String> waveLength;
    // 波长参数
    private List<String> waveLengthParams;
    // 波长公式
    private String waveLengthFormula;
    // 微分方式
    private String differentialMode;
    // 曲线类型
    private String curveType;
    // 方程类型
    private String equationType;
    // 曲线公式
    private String curveFormula;
    // 系数方式(0.直接系数法 1.标准曲线法)
    private String coefficientMode;
    // 系数参数(系数参数是系数方式延伸下来的参数,因为一个字段无法做到只能使用两个字段进行操作)
    private List<String> coefficientParams;
    // 零点方式
    private List<String> zeroMethod;
    // 重复模式
    private String repeatMode;
    // 重复次数
    private String repeatCount;
    // 时间间隔
    private String timeInterval;
    // 统计内容:[平均值,SD,RSD]
    private List<String> statisticalContent;
    // 样品池方式(0: 单池重复后再移动 1: 依次移动并重复)
    private String samplePoolWay;
    // 样品输入参数
    private List<Map<String, Object>> sampleInputParams;
    // 待计算结果
    private List<Map<String, Object>> resultParams;
    // 修约设置
    private List<Map<String, Object>> approxParams;
    // 质控设置
    private List<Map<String, Object>> qcParams;

}
