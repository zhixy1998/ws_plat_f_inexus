package com.persee.workstation.uv_inexus.po.concise.measurementPo.params;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("质控设置")
@Data
public class FQualityConfVO {
    @ApiModelProperty("质控范围")
    private String controlFormula;

    @ApiModelProperty("质控类型")
    private String controlType;

    @ApiModelProperty("超限操作")
    private String overLimitOperation = ""; // 默认空
}
