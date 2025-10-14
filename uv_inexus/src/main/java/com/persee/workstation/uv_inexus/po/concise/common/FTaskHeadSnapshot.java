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

import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.InputParamMapTableHead;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class FTaskHeadSnapshot {

    @ApiModelProperty("任务id")
    private String taskId;

    @ApiModelProperty("任务表头信息，包含除样品和公式外，所有表头")
    private List<FHeadVO> taskHeads;

    @ApiModelProperty("映射关系")
    private List<InputParamMapTableHead> mapRelation;

    @ApiModelProperty("样品表头")
    private List<FHeadVO> sampleHeads;

}
