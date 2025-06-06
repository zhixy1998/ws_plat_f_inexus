package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class DeviceInitInfo {

    @ApiModelProperty("自检任务名")
    private String initProject;

    @ApiModelProperty("自检任务状态")
    private String initStatus;
}
