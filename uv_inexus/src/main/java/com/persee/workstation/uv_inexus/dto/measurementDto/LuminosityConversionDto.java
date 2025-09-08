package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

@Data
public class LuminosityConversionDto {
    // 待处理的数据id
    private String measureDataId;
    // 方法id
    private String methodId;
    // 数据类型(1.Abs<->T% 2.Abs<->R%)
    private String dataType;
    // 分析数据名称
    private String analysisDataName;
    // 任务id
    private String taskId;
}
