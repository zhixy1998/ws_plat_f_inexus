package com.persee.workstation.uv_inexus.po.guide.measurementPo.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ProjectQueryParam {
    @ApiModelProperty(value = "页码，从1开始")
    private Integer page;

    @ApiModelProperty(value = "每页大小")
    private Integer size;

    @ApiModelProperty("精确查询字段")
    private String keyword;

}
