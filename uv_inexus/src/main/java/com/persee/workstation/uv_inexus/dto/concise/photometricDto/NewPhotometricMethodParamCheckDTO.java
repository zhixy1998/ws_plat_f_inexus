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

package com.persee.workstation.uv_inexus.dto.concise.photometricDto;

import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPhotometricParameter;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class NewPhotometricMethodParamCheckDTO {

    @ApiModelProperty("当前方法id")
    private String currentMethodId;

    @ApiModelProperty("当前任务id")
    private String currentTaskId;

    @ApiModelProperty("新方法id，如果有新方法，即使用新方法id，没有则不传")
    private String newMethodId;

    @ApiModelProperty("测量类型")
    private String measureType;

    @ApiModelProperty("方法名称")
    private String methodName;

    @ApiModelProperty("新方法参数")
    private FPhotometricParameter newMethodParam;

    @ApiModelProperty("清除数据范围")
    private String clearDataRange;

    @ApiModelProperty("是否保存原有数据, true-保存，false-不保存")
    private Boolean isSaveDraft;

    @ApiModelProperty("序列号")
    private String deviceSerialNumber;

    @ApiModelProperty("用户")
    private String updateBy;
}
