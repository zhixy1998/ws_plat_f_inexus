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

package com.persee.workstation.uv_inexus.vo.concise.common;

import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class FSamplePoolInfoVO {

    @ApiModelProperty("设备序列号")
    private String deviceSerialNumber;

    @ApiModelProperty("样品池类型")
    private Integer attachmentType;

    @ApiModelProperty("样品池类型名称")
    private String attachmentTypeName;

    @ApiModelProperty("是否自动切换样品池")
    private Boolean isAutoChangePool;

    @ApiModelProperty("当前样品池编号")
    private Integer currentPoolNum;

    @ApiModelProperty("样品池信息")
    private List<FSamplePoolStatus> poolStatusList;

}
