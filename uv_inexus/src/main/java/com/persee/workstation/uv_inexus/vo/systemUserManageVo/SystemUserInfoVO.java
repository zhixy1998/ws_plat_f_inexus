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

package com.persee.workstation.uv_inexus.vo.systemUserManageVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemUserInfoVO {

    @ApiModelProperty("用户名")
    private String userName;

    @ApiModelProperty("用户真实姓名")
    private String userRealName;

    @ApiModelProperty("电话")
    private String phone;

    @ApiModelProperty("用户邮箱")
    private String email;

    @ApiModelProperty("用户职位")
    private String userPosition;

    @ApiModelProperty("组名")
    private String groupName;

    @ApiModelProperty("用户组编号")
    private String groupNo;

    @ApiModelProperty("系统角色")
    private String systemIdentityName;

    @ApiModelProperty("系统角色id")
    private String systemIdentityId;
}
