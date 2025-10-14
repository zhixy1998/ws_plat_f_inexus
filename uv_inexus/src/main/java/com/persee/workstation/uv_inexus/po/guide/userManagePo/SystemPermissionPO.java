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

package com.persee.workstation.uv_inexus.po.guide.userManagePo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("系统权限表")
@TableName("t_system_permission")
public class SystemPermissionPO extends BasePO {

    @ApiModelProperty("仪器类型id")
    @TableField(value = "device_type_id")
    private Integer deviceTypeId;

    @ApiModelProperty("权限序号")
    @TableField(value = "sid")
    private Integer sid;

    @ApiModelProperty("权限名称")
    @TableField(value = "permission_name")
    private String permissionName;

    @ApiModelProperty("权限类型")
    @TableField(value = "permission_type")
    private String permissionType;

    @ApiModelProperty("权限备注")
    @TableField(value = "remark")
    private String remark;
}
