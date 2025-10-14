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

import java.time.LocalDateTime;

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
 * 用户表PO类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("用户表")
@TableName("t_sys_user")
public class SysUserPO extends BasePO {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("用户ID")
    private String id;

    @TableField("username")
    @ApiModelProperty("用户名")
    private String username;

    @TableField("token")
    @ApiModelProperty("token")
    private String token;

    @TableField("password")
    @ApiModelProperty("密码（加密存储）")
    private String password;

    @TableField("position")
    @ApiModelProperty("职务")
    private String position;

    @TableField("remarks")
    @ApiModelProperty("备注")
    private String remarks;

    @TableField("real_name")
    @ApiModelProperty("真实姓名")
    private String realName;

    @TableField("phone")
    @ApiModelProperty("手机号")
    private String phone;

    @TableField("email")
    @ApiModelProperty("邮箱")
    private String email;

    @TableField("status")
    @ApiModelProperty("状态：1-启用，0-禁用")
    private Integer status;

    @TableField("role_id")
    @ApiModelProperty("角色id")
    private Integer roleId;

    @TableField("last_login_time")
    @ApiModelProperty("最后登录时间")
    private LocalDateTime lastLoginTime;
}