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
