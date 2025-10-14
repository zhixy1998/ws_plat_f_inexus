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

package com.persee.workstation.uv_inexus.dto.parseReport;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 质量控制表数据DTO
 */
@ApiModel("质控设置")
@Data
public class QualityControlDTO {
    @ApiModelProperty("质控范围")
    private String controlFormula;
    
    @ApiModelProperty("质控类型")
    private String controlType;
    
    @ApiModelProperty("超限操作")
    private String overLimitOperation = ""; // 默认空
}