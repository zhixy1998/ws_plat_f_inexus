package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

@Data
public class MethodParamsDto extends RationParameter{
    // 用户名
    private String updateBy;
    // 项目id
    private String projectId;
    // 方法id
    private String methodId;
    // 方法名称
    private String methodName;


}
