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

package com.persee.workstation.uv_inexus.vo.concise.photometric;

import com.persee.workstation.uv_inexus.vo.concise.ration.FRepeatMeasureData;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class FPhotometricOriginDataInfo {

    @ApiModelProperty("光度模式")
    private String mode;

    @ApiModelProperty("波段")
    private String wavelength;

//    @ApiModelProperty("abs的数据值")
//    private String absValue;
//
//    @ApiModelProperty("T%的值")
//    private String tPercentValue;
//
//    @ApiModelProperty("ER的值")
//    private String erValue;
//
//    @ApiModelProperty("ES的值")
//    private String esValue;

    @ApiModelProperty("测量数据")
    private Map<String, String> data;

    @ApiModelProperty("其他数据")
    private Map<String, FRepeatMeasureData> otherData;

    @ApiModelProperty("标识")
    private String identity;

    @ApiModelProperty("测量时间")
    private LocalDateTime measureTime;

}
