package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

@Data
public class DataNameValue {

    @ApiModelProperty("键")
    private String dataKey;

    @ApiModelProperty("值")
    private String dataValue;
}
