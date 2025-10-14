/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.dto.measurementDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskManagementDto {
    @ApiModelProperty("任务id")
    private String id;
    @ApiModelProperty("任务名称")
    private String taskName;
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
