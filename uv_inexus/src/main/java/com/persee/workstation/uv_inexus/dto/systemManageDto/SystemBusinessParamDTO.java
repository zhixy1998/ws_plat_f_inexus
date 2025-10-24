/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.dto.systemManageDto;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemBusinessParamDTO {

    @ApiModelProperty("标准曲线配置")
    private String standardCurveConf;

    @ApiModelProperty("显示已删除")
    private Boolean showDeleted;

    @ApiModelProperty("显示样品测量时间")
    private Boolean showMeasureTime;

    @ApiModelProperty("自动保存开关")
    private Boolean autoSaveSwitch;

    @ApiModelProperty("自动保存间隔")
    private String autoSaveTime;

    @ApiModelProperty("自动登出开关")
    private Boolean autoLeaveSwitch;

    @ApiModelProperty("自动登出时间")
    private String autoLeaveTime;

    @TableField("exe_file_path")
    private String filePath;

    @ApiModelProperty("用户")
    private String updateBy;
}
