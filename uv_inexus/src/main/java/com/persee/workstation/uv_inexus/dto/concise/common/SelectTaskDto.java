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

package com.persee.workstation.uv_inexus.dto.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
public class SelectTaskDto {
    @ApiModelProperty("模糊查询任务名")
    private String taskName;
    @ApiModelProperty("模糊查询方法名")
    private String methodName;
    @ApiModelProperty("页数")
    private Integer pageNum;
    @ApiModelProperty("每页展示数")
    private Integer pageSize;
    @ApiModelProperty("方法类型")
    private String methodType;

    @ApiModelProperty("用户名")
    private String updateBy;

}
