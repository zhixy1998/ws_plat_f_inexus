package com.persee.workstation.uv_inexus.dto.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class FSamplePoolInfoChangeDto {
    @ApiModelProperty("设备序列号")
    private String deviceSerialNumber;

    @ApiModelProperty("样品池号")
    private Integer poolNum;

    @ApiModelProperty("是否自动切换样品池")
    private Boolean isAutoChangePool;

    @ApiModelProperty("样品池状态")
    private Boolean poolStatus;

    @ApiModelProperty("样品池用途")
    private String poolPurpose;

    @ApiModelProperty("更新人")
    private String updateBy;
}
