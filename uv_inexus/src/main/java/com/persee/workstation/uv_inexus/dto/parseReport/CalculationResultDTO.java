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
 * 计算公式表数据DTO
 */
@ApiModel("待计算结果")
@Data
public class CalculationResultDTO {
    @ApiModelProperty("标识")
    private String resultIdentity;
    
    @ApiModelProperty("标题")
    private String resultName;
    
    @ApiModelProperty("单位")
    private String unit;
    
    @ApiModelProperty("公式")
    private String formulaText;
}