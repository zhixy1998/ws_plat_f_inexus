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
public class SystemUserGroupInfo {
    @ApiModelProperty("组名称")
    private String groupName;

    @ApiModelProperty("组编号")
    private String groupNo;

    @ApiModelProperty("组描述")
    private String groupDesc;

    @ApiModelProperty("系统身份")
    private Integer systemIdentityId;

    @ApiModelProperty("用户组是否有效")
    private Integer isValid;

}
