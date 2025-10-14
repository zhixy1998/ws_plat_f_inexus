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
public class FSamplePoolInfoChangeDto {
    @ApiModelProperty("设备序列号")
    private String deviceSerialNumber;

    @ApiModelProperty("样品池号")
    private Integer poolNum;

    @ApiModelProperty("是否自动切换样品池")
    private Boolean isAutoChangePool;

    @ApiModelProperty("样品池状态")
    private Boolean poolStatus;

    @ApiModelProperty("样品池用途")
    private Integer poolPurpose;

    @ApiModelProperty("任务id")
    private String taskId;

    @ApiModelProperty("更新人")
    private String updateBy;
}
