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

import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Data
public class FRationAddOrDeleteSampleDTO {
    @ApiModelProperty("任务id")
    private String taskId;

    @ApiModelProperty("方法id")
    private String methodId;

    @ApiModelProperty("更新人")
    private String updateBy;

    @Autowired
    private String deviceSerialNumber;

//    @ApiModelProperty("新增的样本")
//    private List<SampleVO> newSamples;

    @ApiModelProperty("标准样品")
    private List<Map<String, String>> newStandSamples;

    @ApiModelProperty("新增的未知样本")
    private List<Map<String, String>> newUnknowSamples;

    @ApiModelProperty("新增的标准样本表头")
    private List<FHeadVO> standHeads;

    @ApiModelProperty("新增的未知样本表头")
    private List<FHeadVO> unknowHeads;

    @ApiModelProperty("删除的样本id")
    private List<String> delSampleIds;
}
