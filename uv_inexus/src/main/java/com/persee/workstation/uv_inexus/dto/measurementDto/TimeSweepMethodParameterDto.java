package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

/**
 * 时间扫描修改方法参数DTO
 */
@Data
public class TimeSweepMethodParameterDto extends TimeSweepParamter {
    // 项目ID
    private String projectId;
    // 测量类型
    private String measureType;
    // 方法ID
    private String methodId;
    // 方法名称
    private String methodName;
    // 用户名
    private String updateBy;
    // 是否共享
    private String isShare;
}
