package com.persee.workstation.uv_inexus.dto.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SelectTaskDto {
    @ApiModelProperty("模糊查询关键字")
    private String selectKey;
    @ApiModelProperty("页数")
    private Integer pageNum;
    @ApiModelProperty("每页展示数")
    private Integer pageSize;
    @ApiModelProperty("方法类型")
    private String methodType;

    @ApiModelProperty("用户名")
    private String updateBy;

}
