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
 * 检测参数及子表字段DTO
 */
@ApiModel("检测参数及子表字段DTO")
@Data
public class DetectionParametersDTO {
    @ApiModelProperty("测量参数")
    private String detectionParameters; // 主表JSON参数

    @ApiModelProperty("前处理描述（前处理表）")
    private String preTreatmentDesc; // 对应pretreatment_description

    @ApiModelProperty("质控类型（质量控制表）")
    private String qcType; // 对应control_type

    @ApiModelProperty("结果表示（计算公式表）")
    private String resultExpression; // 对应result_expression
}