package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

import java.util.Map;

@Data
public class MethodParamRequestDto {
    private String methodName;  // 测量方法名称
    private String methodType; // 测量类型
    private String projectId;
    private Map<String, Object> params; // 不定参数
}