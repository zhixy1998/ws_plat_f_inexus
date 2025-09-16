package com.persee.workstation.uv_inexus.dto.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class QueryMethodDto {
    @ApiModelProperty("方法类型")
    private String methodType;
    @ApiModelProperty("每页展示条数")

    private Integer pageSize;
    @ApiModelProperty("分页页数")

    private Integer pageNum;
    @ApiModelProperty("用户名")

    private String updateBy;
    @ApiModelProperty("模糊查询关键字")

    private String selectKey;
}
