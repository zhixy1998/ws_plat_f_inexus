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

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class FMethodChangeCheckVO {

    @ApiModelProperty("当前方法id")
    private String currentMethodId;

    @ApiModelProperty("参数是否有改变，true-改变，false-未改变")
    private Boolean paramChange;

    @ApiModelProperty("提示信息")
    private String message;

    @ApiModelProperty("清除数据范围")
    private String clearDataRange;

}
