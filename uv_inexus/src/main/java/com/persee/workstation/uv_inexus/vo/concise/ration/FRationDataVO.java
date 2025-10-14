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

package com.persee.workstation.uv_inexus.vo.concise.ration;

import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FRationDataVO {

    @ApiModelProperty("任务id")
    private String taskId;

    @ApiModelProperty("样品id")
    private String sampleId;

    LocalDateTime startTime;
    LocalDateTime endTime;

    private String sampleTime;

    @ApiModelProperty("机器rsd数据")
    private List<ChangeParam> machineData;

    @ApiModelProperty("原始计算数据")
    private List<FRationWavelengthDataInfo> wavelengthData;

    @ApiModelProperty("计算数据")
    private List<FRationCountDataInfo> countData;

}
