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

package com.persee.workstation.uv_inexus.po.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class FSamplePoolStatus {

    @ApiModelProperty("样品池号")
    private Integer poolNum;

    @ApiModelProperty("样品池号名称")
    private String poolName;

    @ApiModelProperty("样品池用途")
    private Integer poolPurpose;

    @ApiModelProperty("样品池用途名称")
    private String poolPurposeName;

    @ApiModelProperty("样品池状态，true-可用，false-不可用")
    private Boolean poolStatus;

    @ApiModelProperty("样品池数据，比色皿匹配值，待定")
    private String poolData;
}
