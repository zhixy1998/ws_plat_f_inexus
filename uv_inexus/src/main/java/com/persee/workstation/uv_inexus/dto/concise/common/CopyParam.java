package com.persee.workstation.uv_inexus.dto.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class CopyParam {
    @ApiModelProperty("任务id")
    String taskId;
    @ApiModelProperty("任务新名字")
    String taskName;
}
