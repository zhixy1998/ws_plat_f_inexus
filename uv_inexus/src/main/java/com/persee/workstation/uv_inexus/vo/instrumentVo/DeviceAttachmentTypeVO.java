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

package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class DeviceAttachmentTypeVO {

    @ApiModelProperty("附件类型")
    private Integer attachmentNum;

    @ApiModelProperty("附件类型名称")
    private String attachmentName;

    @ApiModelProperty("仪器序列号")
    private String deviceSerialNumber;

    @ApiModelProperty("样本池数量")
    private Integer samplePoolNum;

    @ApiModelProperty("附件种类")
    private Integer attachmentKind;
}
