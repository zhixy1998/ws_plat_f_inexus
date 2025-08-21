package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

/**
 * 微分接口参数
 */
@Data
public class DifferentialDto {
    // 待处理数据id
    private String measureDataId;
    // 微分次数
    private String differentialNum;
    // 点数(点数要比系数小)
    private String pointNum;
    // 系数
    private String coefficient;
    // 分析数据名称
    private String analysisDataName;
    // 任务id
    private String taskId;
}
