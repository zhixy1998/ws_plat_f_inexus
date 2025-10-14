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

package com.persee.workstation.uv_inexus.dto.sysUserPermissionDto;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 组-用户树形结构DTO
 */
@Data
@ApiModel("小组树形结构")
public class GroupTreeDTO {
    @ApiModelProperty("小组ID")
    private String id;

    @ApiModelProperty("小组名称")
    private String groupName;

    @ApiModelProperty("小组描述")
    private String description;

    @ApiModelProperty("小组状态（1-启用，0-禁用）")
    private Integer status;

    @ApiModelProperty("该小组下的用户列表")
    private List<UserTreeDTO> userList;  // 同级包含用户列表
}