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

package com.persee.workstation.uv_inexus.po.concise.measurementPo.params;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("质控设置")
@Data
public class FQualityConfVO {

    @ApiModelProperty("质控类型")
    private String controlType;

    @ApiModelProperty("质控描述")
    private String controlDesc;

    @ApiModelProperty("质控上限值")
    private Double upperLimit;
    @ApiModelProperty("是否包含上限值")
    private Boolean isUpperLimitIncluded;

    @ApiModelProperty("质控下限值")
    private Double lowerLimit;
    @ApiModelProperty("是否包含下限值")
    private Boolean isLowerLimitIncluded;

}
