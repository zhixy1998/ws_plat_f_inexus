package com.persee.workstation.uv_inexus.vo.systemManageVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemBusinessParamVO {

    @ApiModelProperty("参数配置类型")
    private Integer businessType;

    @ApiModelProperty("业务参数名称")
    private String businessName;

    @ApiModelProperty("配置参数")
    private String confParam;

    @ApiModelProperty("配置参数名")
    private String paramName;

    @ApiModelProperty("配置参数类型")
    private Integer paramType;

    @ApiModelProperty("配置参数值")
    private String paramValue;

    @ApiModelProperty("配置参数最小值")
    private String paramMinValue;

    @ApiModelProperty("配置参数最大值")
    private String paramMaxValue;
}
