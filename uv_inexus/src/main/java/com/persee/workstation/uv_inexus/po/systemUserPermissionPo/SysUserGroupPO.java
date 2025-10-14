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
 * 用户-小组关联表PO类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("用户 - 小组关联表")
@TableName("t_sys_user_group")
public class SysUserGroupPO extends BasePO {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("关联ID")
    private String id;

    @TableField("user_id")
    @ApiModelProperty("用户ID，关联用户表")
    private String userId;

    @TableField("group_id")
    @ApiModelProperty("小组ID，关联小组表")
    private String groupId;
}