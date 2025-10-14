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

package com.persee.workstation.uv_inexus.po.systemUserPermissionPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色表PO类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("角色表")
@TableName("t_sys_role")
public class SysRolePO extends BasePO {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("角色ID")
    private String id;

    @TableField("role_name")
    @ApiModelProperty("角色名称")
    private String roleName;

    @TableField("role_code")
    @ApiModelProperty("角色编码")
    private String roleCode;

    @TableField("description")
    @ApiModelProperty("角色描述")
    private String description;

    @TableField("perm_ids")
    @ApiModelProperty("权限json")
    private String permIds;

    @TableField("status")
    @ApiModelProperty("状态：1-启用，0-禁用")
    private Integer status;
}