package com.persee.workstation.uv_inexus.dto.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class FQueryMethodWithPageDto {

    @ApiModelProperty("页码")
    private Integer pageNum;

    @ApiModelProperty("每页展示数量")
    private Integer pageSize;

    @ApiModelProperty("更新人")
    private String updateBy;

    @ApiModelProperty("测量类型")
    private String measureType;

    @ApiModelProperty("方法名称")
    private String methodName;
}
