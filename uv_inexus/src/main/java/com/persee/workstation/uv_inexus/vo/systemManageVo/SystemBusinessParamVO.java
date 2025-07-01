package com.persee.workstation.uv_inexus.vo.systemManageVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemBusinessParamVO {

    @ApiModelProperty("峰间距")
    private String peakInterval;

    @ApiModelProperty("峰谷差")
    private String peakValleyDiff;
}
