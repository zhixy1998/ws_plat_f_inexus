package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MethodParamRequestDto {
    private String methodName;  // 测量方法名称
    private String methodType; // 测量类型
    private String projectId; // 项目id
    private List<String> pretreatmentIds; // 前处理的多个id
    private String updateBy; // 修改人
    private Map<String, Object> params; // 不定参数
}