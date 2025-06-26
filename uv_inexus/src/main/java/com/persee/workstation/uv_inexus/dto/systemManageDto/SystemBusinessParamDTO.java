package com.persee.workstation.uv_inexus.dto.systemManageDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemBusinessParamDTO {

    @ApiModelProperty("参数配置类型")
    private Integer businessType;

    @ApiModelProperty("配置参数")
    private String confParam;

    @ApiModelProperty("配置参数值")
    private String paramValue;

    @ApiModelProperty("用户")
    private String updateBy;
}
