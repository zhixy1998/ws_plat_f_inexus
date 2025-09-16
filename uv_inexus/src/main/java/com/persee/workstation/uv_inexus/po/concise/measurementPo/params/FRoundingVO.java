package com.persee.workstation.uv_inexus.po.concise.measurementPo.params;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("修约设置")
@Data
public class FRoundingVO {
    @ApiModelProperty("修约变量")
    private String resultIdentity;

    @ApiModelProperty("修约方式")
    private String roundingType;

    @ApiModelProperty("修约条件")
    private String roundingCondition;

    @ApiModelProperty("小数位数")
    private Integer decimalPlaces;
}
