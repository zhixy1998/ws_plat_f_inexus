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

package com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("样品信息")
public class FSampleVO {

    @ApiModelProperty("样品ID")
    private String sampleId;

    @ApiModelProperty("样品编号")
    private String sampleNumber;

    @ApiModelProperty("样品名称")
    private String sampleName;

    @ApiModelProperty("样品类型")
    private String sampleType;

    @ApiModelProperty("样品种类,0-标准样品，1-未知样品")
    private String sampleKind;

    @ApiModelProperty("标准浓度")
    private String normalConcentration;

    @ApiModelProperty("稀释比")
    private String dilutionRatio;

    @ApiModelProperty("样品位置")
    private String sampleLocation;

    @ApiModelProperty("平行样组id")
    private String parallelGroupId;

    // 取样量
    @ApiModelProperty("取样量")
    private String sampleVolume;

}
