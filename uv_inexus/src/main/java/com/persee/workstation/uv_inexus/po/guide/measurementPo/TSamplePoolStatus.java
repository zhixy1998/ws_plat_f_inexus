package com.persee.workstation.uv_inexus.po.guide.measurementPo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class TSamplePoolStatus {

    @ApiModelProperty("样品池号")
    private Integer poolNum;

    @ApiModelProperty("样品池号名称")
    private String poolName;

    @ApiModelProperty("样品池用途")
    private Integer poolPurpose;

    @ApiModelProperty("样品池用途名称")
    private String poolPurposeName;

    @ApiModelProperty("样品池状态，true-可用，false-不可用")
    private Boolean poolStatus;

    @ApiModelProperty("样品池数据，比色皿匹配值，待定")
    private String poolData;
}
