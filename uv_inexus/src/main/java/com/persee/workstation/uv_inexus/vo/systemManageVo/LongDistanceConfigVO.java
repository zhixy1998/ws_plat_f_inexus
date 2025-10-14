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
public class LongDistanceConfigVO {
    //是否允许远程控制
    @ApiModelProperty("是否允许远程控制")
    private Integer remote_control;
    //是否远程数据访问
    @ApiModelProperty("是否远程数据访问")
    private Integer remote_data_access;
}
