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

import java.util.List;

@Data
public class EqualSampleParam {
    @ApiModelProperty("平行样唯一标识'")
    private String equalSampleKey;
    @ApiModelProperty("要加入平行样的样品id")
    private List<String> sampleIds;
    @ApiModelProperty("操作人")
    private String updateBy;
}
