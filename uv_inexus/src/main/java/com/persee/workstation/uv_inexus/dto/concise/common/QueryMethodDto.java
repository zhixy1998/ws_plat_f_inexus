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

@Data
public class QueryMethodDto {
    @ApiModelProperty("方法类型")
    private String methodType;
    @ApiModelProperty("每页展示条数")

    private Integer pageSize;
    @ApiModelProperty("分页页数")

    private Integer pageNum;
    @ApiModelProperty("用户名")

    private String updateBy;
    @ApiModelProperty("模糊查询关键字")

    private String selectKey;
}
