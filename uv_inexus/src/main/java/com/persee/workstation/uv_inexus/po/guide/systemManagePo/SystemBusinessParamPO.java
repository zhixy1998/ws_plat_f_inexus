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

package com.persee.workstation.uv_inexus.po.guide.systemManagePo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_system_business_param")
@ApiModel("系统业务参数")
public class SystemBusinessParamPO extends BasePO {

    @ApiModelProperty("标准曲线配置, 1-显示R,2-显示R2")
    @TableField("standard_curve_conf")
    private String standardCurveConf;

    @ApiModelProperty("显示已删除")
    @TableField("show_deleted")
    private Boolean showDeleted;

    @ApiModelProperty("显示样品测量时间")
    @TableField("show_measure_time")
    private Boolean showMeasureTime;

    @ApiModelProperty("自动保存开关")
    @TableField("auto_save_switch")
    private Boolean autoSaveSwitch;

    @ApiModelProperty("自动保存间隔")
    @TableField("auto_save_time")
    private String autoSaveTime;

    @ApiModelProperty("自动登出开关")
    @TableField("auto_leave_switch")
    private Boolean autoLeaveSwitch;

    @ApiModelProperty("自动登出时间")
    @TableField("auto_leave_time")
    private String autoLeaveTime;

    @TableField("exe_file_path")
    @ApiModelProperty("exe文件路径")
    private String filePath;

}
