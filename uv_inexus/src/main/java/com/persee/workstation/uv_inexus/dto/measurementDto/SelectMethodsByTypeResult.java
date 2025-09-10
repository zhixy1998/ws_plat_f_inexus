package com.persee.workstation.uv_inexus.dto.measurementDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class SelectMethodsByTypeResult {
    @ApiModelProperty("方法数量")
    private Integer methodNumber;
    @ApiModelProperty("方法类型")
    private String methodtype;
}
