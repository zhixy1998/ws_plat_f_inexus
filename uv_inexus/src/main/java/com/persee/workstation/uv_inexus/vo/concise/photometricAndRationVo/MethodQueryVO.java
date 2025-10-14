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

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MethodQueryVO {

    @ApiModelProperty("方法名")
    private String methodName;

    @ApiModelProperty("保存时间")
    private LocalDateTime createTime;

    @ApiModelProperty("最近使用时间")
    private LocalDateTime methodUseTime;

    @ApiModelProperty("方法参数")
    private JSONObject methodParam;
}
