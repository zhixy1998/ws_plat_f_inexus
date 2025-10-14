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

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 多子表多条数据查询DTO
 */
@ApiModel("多子表多条数据查询DTO")
@Data
public class MultiTableDataDTO {

    @ApiModelProperty("波长列表（波长1、波长2、波长3的值）")
    private List<String> detectionParameters; // 原detectionParameters改造为波长列表

    @ApiModelProperty("波长参数列表（波长参数1、波长参数2的值）")
    private List<String> wavelengthParamList; // 新增字段

    @ApiModelProperty("待计算结果列表（计算公式表）")
    private List<CalculationResultDTO> calculationResults;

    @ApiModelProperty("修约设置列表（修约表）")
    private List<RoundingSettingDTO> roundingSettings;

    @ApiModelProperty("输入参数列表（计算公式表）")
    private List<InputParamDTO> inputParams;

    @ApiModelProperty("前处理描述列表（前处理表）")
    private List<PretreatmentDTO> pretreatments;

    @ApiModelProperty("质量控制列表（质量控制表）")
    private List<QualityControlDTO> qualityControls;
}