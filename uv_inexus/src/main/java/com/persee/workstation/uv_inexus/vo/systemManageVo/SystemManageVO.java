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

package com.persee.workstation.uv_inexus.vo.systemManageVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemManageVO {
    @ApiModelProperty("语言")
    private String language;

    @ApiModelProperty("远程控制")
    private Boolean remoteControl;

    @ApiModelProperty("远程控制数量上限")
    private Integer remoteControlNum;

    @ApiModelProperty("远程数据访问")
    private Boolean remoteDataAccess;

    @ApiModelProperty("远程数据访问用户上限")
    private Integer remoteDataAccessNum;

//    @ApiModelProperty("仪器使用模式")
//    private String deviceUseMode;
//
//    @ApiModelProperty("仪器资源自动释放时间")
//    private Integer deviceResourceAutoReleaseTime;
}
