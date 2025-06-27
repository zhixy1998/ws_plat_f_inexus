package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

import java.util.List;

@Data
public class RationParameter extends MeasurePublicParameters{
    // 波长值
    private List<String> waveLength;
    // 波长参数
    private List<String> waveLengthParams;
    // 波长公式
    private String waveLengthFormula;
    //微分方式
    private String differentialMode;
    // 曲线类型
    private String curveType;
    // 方程类型
    private String equationType;
    // 曲线公式
    private String curveFormula;
    // 系数方式(0.直接系数法 1.标准曲线法)
    private String coefficientMode;
    // 是否插入零点
    private String isInsertZero;
    // 是否强制过零点
    private String isForceZero;
    // 重复模式
    private String repeatMode;
    // 重复次数
    private String repeatCount;
    // 时间间隔
    private String timeInterval;
    // 是否平均值
    private String isAverage;
    // 是否SD
    private String isSD;
    // 是否RSD
    private String isRSD;
    // 样品池方式(0: 单池重复后再移动 1: 依次移动并重复)
    private String samplePoolWay;
    // 样品输入参数
    private List<String> sampleInputParams;
    // 待计算结果
    private List<String> resultParams;
    // 修约设置
    private List<String> approxParams;
    // 质控设置
    private List<String> qcParams;

}
