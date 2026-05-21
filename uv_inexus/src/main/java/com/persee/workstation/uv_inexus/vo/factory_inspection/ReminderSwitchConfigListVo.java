package com.persee.workstation.uv_inexus.vo.factory_inspection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("提醒开关配置列表VO")
public class ReminderSwitchConfigListVo {

    @ApiModelProperty("总开关配置")
    private ReminderSwitchConfigVo globalSwitch;

    @ApiModelProperty("分项提醒配置列表")
    private List<ReminderSwitchConfigVo> itemConfigs;
}
