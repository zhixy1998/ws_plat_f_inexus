package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 光度测量参数
 */
@Data
public class PhotometricParameter extends MeasurePublicParameters {
    // 光度模式
    private String luminosity;
    // 波长值(波长值是多个值,不连续的)
    private List<String> waveLengths;
    // 重复模式
    private String repetitionPattern;
    // 重复次数
    private String repetitionTime;
    // 时间间隔
    private String timeInterval;
    // 统计内容:[平均值,SD,RSD]
    private List<String> statisticalContent;
    // 样品池方式(0: 单池重复后再移动 1: 依次移动并重复)
    private String samplePoolWay;
    // 输入参数
    private List<Map<String, Object>> inputParams;
    // 待计算结果
    private List<Map<String, Object>> resultParams;
    // 修约设置
    private List<Map<String, Object>> roundingSettings;
    // 质控设置
    private List<Map<String, Object>> qualityControlSettings;

}
