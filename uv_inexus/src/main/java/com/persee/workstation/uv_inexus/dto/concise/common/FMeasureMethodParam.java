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

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FMeasureMethodParam {
    @ApiModelProperty("id")
    private String id;
    @ApiModelProperty("前处理Key")

    // 前处理Key
    private String preprocessKey;
    @ApiModelProperty("多个计算公式Key")

    // 多个计算公式Key

    private String multipleCalculationFormulaKey;
    @ApiModelProperty("所属项目Key(当选用的是公共方法则项目key为空)")

    // 所属项目Key(当选用的是公共方法则项目key为空)
    private String projectItemKey;
    @ApiModelProperty("方法名称")

    // 方法名称
    private String methodName;
    @ApiModelProperty("测量类型")

    private String measurementType;

    @ApiModelProperty("测量参数信息")
    private JSONObject measurementParamInfo;

    @ApiModelProperty("是否共享方法(0.否 1.是)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String isSharingMethod;

    @ApiModelProperty("方法创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime methodCreateTime;

    @ApiModelProperty("方法创建人")
    private String methodCreator;

    @ApiModelProperty("方法使用时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime methodUseTime;

    @ApiModelProperty("是否草稿(0.不是草稿 1.是草稿)")
    private String isDraft;

    @ApiModelProperty("使用状态")
    private String useState;
}
