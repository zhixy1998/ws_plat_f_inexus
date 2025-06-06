package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataNameValue {

    @ApiModelProperty("键")
    private String key;

    @ApiModelProperty("值")
    private String value;
}
