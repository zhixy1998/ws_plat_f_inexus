package com.persee.workstation.uv_inexus.dto.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class UpdateTaskDto {
    @ApiModelProperty("方法Id")
    private String taskId;
    @ApiModelProperty("方法名称")
    private String taskName;
}
