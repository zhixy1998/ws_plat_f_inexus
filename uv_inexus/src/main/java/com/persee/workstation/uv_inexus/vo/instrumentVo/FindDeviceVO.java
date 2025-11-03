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
public class FindDeviceVO {

    @ApiModelProperty("序列号")
    private String deviceSerialNumber;

    @ApiModelProperty(value = "自检状态值",example = "0：未完成；1：自检成功； 2：自检失败； 3：自检进行中")
    private Integer isInited;

    @ApiModelProperty(value = "开机状态",example = "0-无响应，1-已开机，2-开机中，3-软关机")
    private Integer isDevOpened;

    @ApiModelProperty("机器ip地址")
    private String ip;

    @ApiModelProperty("附件类型")
    private Integer attachmentType;

    @ApiModelProperty("附件种类")
    private Integer attachmentKind;

    @ApiModelProperty("附件名称")
    private String attachmentName;

    @ApiModelProperty("当前样品池位置")
    private Integer currentPoolNum;

    @ApiModelProperty("总池数")
    private Integer totalPoolNum;

    @ApiModelProperty("仪器名称")
    private String deviceName;

    @ApiModelProperty("仪器型号名称")
    private String deviceModelName;

    @ApiModelProperty("计算公式")
    private String linearSegment;
}
