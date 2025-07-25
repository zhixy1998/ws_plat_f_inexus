package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class TaskBaseVO {

    @ApiModelProperty("方法ID")
    private String taskId;

    @ApiModelProperty("方法名称")
    private String taskName;
}
