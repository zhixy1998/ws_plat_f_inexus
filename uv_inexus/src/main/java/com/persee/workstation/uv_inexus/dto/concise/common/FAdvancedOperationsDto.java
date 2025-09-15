package com.persee.workstation.uv_inexus.dto.concise.common;

import lombok.Data;

@Data
public class FAdvancedOperationsDto {
    // 待处理数据id
    private String measureDataId;
    // 数据类型(4.对数 5.倒数)
    private String dataType;
    // 分析数据名称
    private String analysisDataName;
    // 任务id
    private String taskId;
}
