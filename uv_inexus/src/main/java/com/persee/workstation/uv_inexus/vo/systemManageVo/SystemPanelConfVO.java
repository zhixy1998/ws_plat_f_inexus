package com.persee.workstation.uv_inexus.vo.systemManageVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemPanelConfVO {

    @ApiModelProperty("方法列表面板开关")
    private Boolean methodListSwitch;

    @ApiModelProperty("项目统计面板开关")
    private Boolean projectStatisticsSwitch;

    @ApiModelProperty("任务列表面板开关")
    private Boolean taskListSwitch;
}
