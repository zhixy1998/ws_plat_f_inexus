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

package com.persee.workstation.uv_inexus.vo.concise.photometric;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureSample;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FPhotometricSampleAndDataVO {

    private String taskId;

    private TMeasureTask task;

    private TMeasureSample sample;

    private String sampleId;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("采集时间")
    private String sampleTime;

    @ApiModelProperty("仪器原始数据，保存使用，不展示给前端")
    @JsonIgnore
    private List<ChangeParam> machineData;

    @ApiModelProperty("原始计算数据")
    private List<FPhotometricOriginDataInfo> wavelengthData;

    @ApiModelProperty("经过公式计算后的数据")
    private List<FPhotometricCountDataInfo> countData;
}

