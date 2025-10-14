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

package com.persee.workstation.uv_inexus.po.guide.instrumentPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "t_int_injection_tray_param")
@ApiModel("进样盘参数设置")
public class InjectionTrayParamPO extends BasePO {

    @TableField(value = "device_serial_number")
    @ApiModelProperty("序列号")
    private String deviceSerialNumber;

    @ApiModelProperty("盘区设置, L,M,R")
    @TableField(value = "panel")
    private String panel;

    @ApiModelProperty("选中盘架与否，0-否，1-是")
    @TableField(value = "is_selected")
    private Integer isSelected;

    @ApiModelProperty("盘架设定")
    @TableField(value = "plate_rail")
    private Integer plateRail;

    @ApiModelProperty("编号信息")
    @TableField(value = "num_info")
    private String numInfo;


}
