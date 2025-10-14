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
 * 修约表数据DTO
 */
@ApiModel("修约设置")
@Data
public class RoundingSettingDTO {
    @ApiModelProperty("修约变量")
    private String resultIdentity;
    
    @ApiModelProperty("修约方式")
    private String roundingType;
    
    @ApiModelProperty("修约条件")
    private String roundingCondition;
    
    @ApiModelProperty("小数位数")
    private Integer decimalPlaces;
}