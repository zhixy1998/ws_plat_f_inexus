package com.persee.workstation.uv_inexus.dto.systemManageDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemPanelConfDTO {
    @ApiModelProperty("方法列表面板开关")
    private Boolean methodListSwitch;

    @ApiModelProperty("项目统计面板开关")
    private Boolean projectStatisticsSwitch;

    @ApiModelProperty(value = "用户")
    private String updateBy;
}
