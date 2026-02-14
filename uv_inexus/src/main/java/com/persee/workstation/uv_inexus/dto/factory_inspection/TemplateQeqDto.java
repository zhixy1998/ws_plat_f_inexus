package com.persee.workstation.uv_inexus.dto.factory_inspection;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class TemplateQeqDto {
    /**
     * 分页字段
     */
    private Integer pageNum;
    private Integer pageSize;

    /**
     * 查询字段
     */
    @ApiModelProperty(value = "查询字段")
    private String query;

    /**
     * 修改者字段
     */
    @ApiModelProperty(value = "修改者字段")
    private String updateBy;
    /**
     * 排序方式
     */
    @ApiModelProperty(value = "排序方式")
    private String sort;
}
