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

package com.persee.workstation.uv_inexus.dto.concise.rationDto;

import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParamMapTableHead;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class FTaskHeadSnapshotDTO {
    @ApiModelProperty(value = "任务id", required = true)
    private String taskId;

    @ApiModelProperty(value = "样品种类", required = true)
    private String sampleKind;

    @ApiModelProperty("所有表头")
    private List<FHeadVO> taskHeads;

    @ApiModelProperty("映射关系")
    private List<FInputParamMapTableHead> mapRelation;
}
