package com.persee.workstation.uv_inexus.dto.systemManageDto;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class InitSystemConf {

    @ApiModelProperty("自动进入实验测试开关")
    @TableField(value = "auto_measure")
    private Boolean autoMeasure;

    @ApiModelProperty("快捷登录开关")
    private Boolean fastLoginSwitch;

    @ApiModelProperty("自动开机开关")
    @TableField("auto_start")
    private Boolean autoStart;


}
