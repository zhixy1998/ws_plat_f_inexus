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
 * 检测报告列表DTO（去重后）
 */
@Data
@ApiModel("检测报告列表返回DTO")
public class DetectionListDTO {
    private String id;
    
    @ApiModelProperty("行业")
    private String industry;

    @ApiModelProperty("标准名称")
    private String standardName;

    @ApiModelProperty("标准号（去重依据）")
    private String standardNumber;

    @ApiModelProperty("发布时间")
    private String releaseDate; // 保持与PO一致的String类型

    @ApiModelProperty("失效时间")
    private String expirationDate;

    @ApiModelProperty("实施时间")
    private String effectiveDate;

    @ApiModelProperty("行业类型")
    private String industryType;

    @ApiModelProperty("状态")
    private String releaseStatus;

    @ApiModelProperty("标准号出现次数（统计字段）")
    private Integer count;
}