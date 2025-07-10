package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

@Data
public class FourOperationsParamDto {
    // 测量数据id1
    private String measureDataId1;
    // 测量数据id2
    private String measureDataId2;
    // 四则运算符(0.加,1.减,2.乘,3.除)
    private String arithmeticOperators;
    // 数据类型(0.数值,1.谱图)
    private String dataType;
    // 分析数据名称
    private String analysisDataName;

}
