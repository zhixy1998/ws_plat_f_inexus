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

package com.persee.workstation.uv_inexus.dto.systemUserManageDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AddSystemPermissionDTO {

    @ApiModelProperty("权限名称")
    private String permissionName;

    @ApiModelProperty("权限类型")
    private String permissionType;

    @ApiModelProperty("权限对应的接口名称")
    private String interfaceName;

    @ApiModelProperty("权限备注")
    private String remark;

    @ApiModelProperty("当前用户")
    private String updateBy;
}
