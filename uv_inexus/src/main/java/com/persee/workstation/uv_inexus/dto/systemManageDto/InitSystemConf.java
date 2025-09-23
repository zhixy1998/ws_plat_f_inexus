package com.persee.workstation.uv_inexus.dto.systemManageDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class InitSystemConf {

    @ApiModelProperty("自动进入实验测试开关")
    private Boolean autoMeasure;

    @ApiModelProperty("快捷登录开关")
    private Boolean fastLoginSwitch;

    @ApiModelProperty("自动开机开关")
    private Boolean autoStart;

    @ApiModelProperty("经典模式或者向导模式开关  1：经典模式 2：向导模式")
    private Integer modeType;


}
