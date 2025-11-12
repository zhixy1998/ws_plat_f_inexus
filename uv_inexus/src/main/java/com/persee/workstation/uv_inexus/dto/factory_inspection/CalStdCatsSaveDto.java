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

package com.persee.workstation.uv_inexus.dto.factory_inspection;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@ApiModel("标准物质分类")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalStdCatsSaveDto {
    /**
     * 标准物质ID
     */
    @ApiModelProperty(value="标准物质ID")
    private String id;

    /**
     * 标准物质名称
     */
    @ApiModelProperty(value="标准物质名称")
    private String name;

    /**
     * 标准物质描述
     */
    @ApiModelProperty(value="标准物质描述")
    private String remark ;

    /**
     * 特征值
     */
    @ApiModelProperty(value="特征值")
    private String characteristicValue;

    /**
     * 标准值
     */
    @ApiModelProperty(value="标准值")
    private String standardValue;
}