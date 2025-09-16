package com.persee.workstation.uv_inexus.dto.concise.rationDto;

import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FRationParameter;
import lombok.Data;

@Data
public class FRationMethodParameterDto extends FRationParameter {
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
