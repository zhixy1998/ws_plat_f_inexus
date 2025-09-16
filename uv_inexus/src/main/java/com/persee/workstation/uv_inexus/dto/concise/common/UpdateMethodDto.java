package com.persee.workstation.uv_inexus.dto.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class UpdateMethodDto {
    @ApiModelProperty("方法Id")
    private String methodId;
    @ApiModelProperty("方法名称")
    private String methodName;
}
