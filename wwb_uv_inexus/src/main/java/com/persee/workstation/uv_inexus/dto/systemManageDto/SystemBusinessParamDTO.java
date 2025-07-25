package com.persee.workstation.uv_inexus.dto.systemManageDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemBusinessParamDTO {

    @ApiModelProperty("峰间距")
    private String peakInterval;

    @ApiModelProperty("峰谷差")
    private String peakValleyDiff;

    @ApiModelProperty("用户")
    private String updateBy;
}
