package com.persee.workstation.uv_inexus.dto.measurementDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class MethodParameterDto {
    @ApiModelProperty("显示标题")
    private String methodProject;
    @ApiModelProperty("标记")
    private String methodMark;
    @ApiModelProperty("单位")
    private String unit;
}
