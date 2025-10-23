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

import java.util.List;

@Data
public class DeviceInfoVO {

    @ApiModelProperty("用户名")
    private String userName;

    @ApiModelProperty("设备名称")
    private String deviceName;

    @ApiModelProperty("序列号")
    private String deviceSerialNumber;

    @ApiModelProperty(value = "开机状态", example = "0-未开机，1-已开机")
    private Integer isDevOpened;

    @ApiModelProperty(value = "自检状态", example = "0：未完成；1：自检成功； 2：自检失败； 3：自检进行中")
    private Integer isInited;

    @ApiModelProperty(value = "联机信息", example = "connect to device：ip=1.1.1.2 port=30500，连接中")
    private String connectMessage;

    @ApiModelProperty(value = "联机状态", example = "0-未连接，1-联机中，2-联机成功，3-联机失败")
    private Integer connectStatus;

    @ApiModelProperty("附加信息")
    private String appendMessage;

    @ApiModelProperty("初始化信息表")
    private List<DeviceInitInfo> intMessage;


}
