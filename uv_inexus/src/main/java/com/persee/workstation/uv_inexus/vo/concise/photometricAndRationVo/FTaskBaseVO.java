package com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class FTaskBaseVO {

    @ApiModelProperty("方法ID")
    private String taskId;

    @ApiModelProperty("方法名称")
    private String taskName;
}
