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

package com.persee.workstation.uv_inexus.dto.instrumentDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DeviceUsageCountDTO {

    @ApiModelProperty("起始时间")
    private LocalDate startTime;

    @ApiModelProperty("终止时间")
    private LocalDate endTime;

    @ApiModelProperty("用户名")
    private String userName;

    @ApiModelProperty("用户密码")
    private String password;

    @ApiModelProperty("需要统计的仪器序列号列表")
    private List<String> deviceSerialNumber;

    @ApiModelProperty("统计类型，需要获取哪些统计信息")
    private List<Integer> countType;

    @ApiModelProperty("查询字段")
    private String search;
}
