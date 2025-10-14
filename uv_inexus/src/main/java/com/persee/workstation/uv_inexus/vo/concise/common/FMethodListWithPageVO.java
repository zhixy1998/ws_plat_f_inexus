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

package com.persee.workstation.uv_inexus.vo.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class FMethodListWithPageVO {

    @ApiModelProperty("总记录数")
    protected long total;

    @ApiModelProperty("每页记录数")
    protected long size;

    @ApiModelProperty("当前页码")
    protected long current;

    @ApiModelProperty("方法信息")
    protected List<FMethodInfoVO> records;
}
