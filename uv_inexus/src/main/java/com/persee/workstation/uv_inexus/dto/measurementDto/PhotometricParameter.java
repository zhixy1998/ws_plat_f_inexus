package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

import java.util.List;

@Data
public class PhotometricParameter extends MeasurePublicParameters {
    // 光度模式
    private String luminosity;
    // 波长值
    private String waveLength;
    // 重复模式
    private String repetitionPattern;
    // 重复次数
    private String repetitionTime;
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
