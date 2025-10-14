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
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_system_role")
public class SystemRolePO extends BasePO {

    @ApiModelProperty("系统角色名称")
    @TableField(value = "system_role_name")
    private String systemRoleName;

    @ApiModelProperty("系统角色编号")
    @TableField(value = "system_role_num")
    private String systemRoleNum;

    @ApiModelProperty("系统权限列表")
    @TableField(value = "permission_list")
    private String permissionList;
}
