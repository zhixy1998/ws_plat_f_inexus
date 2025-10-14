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

package com.persee.workstation.uv_inexus.po.guide.systemUserManagePo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("系统用户管理")
@TableName("t_system_user_manage")
public class SystemUserManagePO extends BasePO {

    @ApiModelProperty("用户名")
    @TableField("user_name")
    private String userName;

    @ApiModelProperty("用户密码")
    @TableField("password")
    private String password;

    @ApiModelProperty("用户当前token")
    @TableField("token")
    private String token;

    @ApiModelProperty("用户真实姓名")
    @TableField("user_real_name")
    private String userRealName;

    @ApiModelProperty("电话")
    @TableField(value = "phone")
    private String phone;

    @ApiModelProperty("用户邮箱")
    @TableField("email")
    private String email;

    @ApiModelProperty("用户职位")
    @TableField("user_position")
    private String userPosition;

    @ApiModelProperty("用户备注")
    @TableField("remark")
    private String remark;

    @ApiModelProperty("组名")
    @TableField(value = "group_name")
    private String groupName;

    @ApiModelProperty("用户组id")
    @TableField("group_no")
    private String groupNo;

    @ApiModelProperty("系统标识id")
    @TableField("system_identity_id")
    private String systemIdentityId;

    @ApiModelProperty("用户是否有效")
    @TableField("is_valid")
    private Boolean isValid;

    @ApiModelProperty("用户是否登录")
    @TableField("is_login")
    private Boolean isLogin;

    @ApiModelProperty("用户登录失败次数")
    @TableField("login_fail_count")
    private Integer loginFailCount;

    @ApiModelProperty("用户修改密码时间")
    @TableField("change_password_time")
    private LocalDateTime changePasswordTime;

    @ApiModelProperty("修改原因")
    @TableField("reason")
    private String reason;

    @ApiModelProperty("用户权限列表")
    @TableField("user_permission_list")
    private String userPermissionList;
}
