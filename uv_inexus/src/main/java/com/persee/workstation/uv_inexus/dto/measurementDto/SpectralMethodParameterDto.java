package com.persee.workstation.uv_inexus.dto.measurementDto;


import lombok.Data;

@Data
public class SpectralMethodParameterDto extends SpectrumParameter{
    // 项目ID
    private String projectId;
    // 方法id
    private String methodId;
    // 测量类型
    private String measureType;
    // 方法名称
    private String methodName;
    // 是否共享
    private String isShare;
}