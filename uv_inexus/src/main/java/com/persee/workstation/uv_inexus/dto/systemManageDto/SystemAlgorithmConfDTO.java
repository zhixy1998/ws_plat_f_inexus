package com.persee.workstation.uv_inexus.dto.systemManageDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemAlgorithmConfDTO {
    @ApiModelProperty(value = "算法类型", required = true)
    private Integer algorithmType;

    @ApiModelProperty(value = "参数，配置对应的参，后端查询使用",required = true)
    private String confParam;

    @ApiModelProperty(value = "用户值", required = true)
    private String confValue;

    @ApiModelProperty(value = "用户")
    private String updateBy;
}
