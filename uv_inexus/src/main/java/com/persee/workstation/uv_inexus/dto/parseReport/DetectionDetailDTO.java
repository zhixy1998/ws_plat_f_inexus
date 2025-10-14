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
import java.util.Map;

import com.persee.workstation.uv_inexus.po.guide.parseReport.Detection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("检测详情DTO")
@Data
public class DetectionDetailDTO {
    @ApiModelProperty("主数据信息")
    private DetectionListDTO mainData;

    @ApiModelProperty("原始数据列表")
    private List<Detection> originalDataList;

    @ApiModelProperty("标准标识、检测类型及id数组")
    private List<Map<String, Object>> identityTypeList; // 包含id
}