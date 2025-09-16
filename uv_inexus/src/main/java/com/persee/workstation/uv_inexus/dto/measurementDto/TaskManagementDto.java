package com.persee.workstation.uv_inexus.dto.measurementDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskManagementDto {
    @ApiModelProperty("所在项目")
    private String projectName;
    @ApiModelProperty("起始时间")
    private LocalDateTime startTime;
    @ApiModelProperty("样品个数")
    private String sampleKey;
    @ApiModelProperty("使用方法")
    private String methodName;
    @ApiModelProperty("测量模式")
    private String measureType;


}
