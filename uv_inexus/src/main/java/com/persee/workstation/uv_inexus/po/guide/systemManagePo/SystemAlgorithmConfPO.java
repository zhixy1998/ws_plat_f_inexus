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
@ApiModel("系统算法配置")
@TableName("t_system_algorithm_conf")
public class SystemAlgorithmConfPO extends BasePO {

    @ApiModelProperty("透过率门限系数k")
    @TableField("transmittance_k")
    private String transmittance;

    @ApiModelProperty("积分球门限系数k")
    @TableField("integrating_sphere_k")
    private String integratingSphere;

    @ApiModelProperty("Abs门限系数k")
    @TableField("abs_k")
    private String abs;

    @ApiModelProperty("峰间距")
    @TableField("peak_interval")
    private String peakInterval;

    @ApiModelProperty("峰谷差")
    @TableField("peak_valley_diff")
    private String peakValleyDiff;
}
