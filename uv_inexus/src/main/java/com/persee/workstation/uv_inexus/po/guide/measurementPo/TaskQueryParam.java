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

package com.persee.workstation.uv_inexus.po.guide.measurementPo;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("任务查询参数")
public class TaskQueryParam {

    @ApiModelProperty(value = "页码，从1开始")
    private Integer page;

    @ApiModelProperty(value = "每页大小")
    private Integer size;

  // @ApiModelProperty(value = "排序字段")
  // private String sortBy;

  // @ApiModelProperty(value = "排序方向: asc-升序, desc-降序")
  // private String sortOrder;

    @ApiModelProperty("模糊查询字段")
    private String keyword;

    @ApiModelProperty("根据类型分类查询")
    private String selectType;
}
