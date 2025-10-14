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

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@ApiModel("用户组配置")
@Data
@TableName("t_gmp_user_group")
public class UserGroupConfPO extends BasePO {

    @ApiModelProperty("组名称")
    @TableField(value = "user_type")
    private String userType;

    @ApiModelProperty("密码时效性，不设置为0，以天为单位")
    @TableField(value = "password_valid")
    private Integer passwordValid;

    @ApiModelProperty("密码复杂度")
    @TableField(value = "password_rule")
    private String passwordRule;

    @ApiModelProperty("登录错误次数")
    @TableField(value = "login_error_time")
    private Integer loginErrorTime;

    @ApiModelProperty("登录错误处理")
    @TableField(value = "login_error_done")
    private String loginErrorDone;

    @ApiModelProperty("用户组是否有效")
    @TableField(value = "is_valid")
    private Integer isValid;

    @ApiModelProperty("用户组备注")
    @TableField(value = "remark")
    private String remark;

    @ApiModelProperty("操作原因")
    @TableField(value = "reason")
    private String reason;

    @ApiModelProperty("系统权限列表")
    @TableField(value = "system_permission_list")
    private String systemPermissionList;

    @ApiModelProperty("系统权限列表")
    @TableField(value = "system_permission_list", exist = false)
    private List<String> permissionList;

}
