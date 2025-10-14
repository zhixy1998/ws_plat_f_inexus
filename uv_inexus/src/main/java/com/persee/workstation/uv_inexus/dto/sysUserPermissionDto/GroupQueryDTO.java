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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 小组分页查询数据传输对象
 */
@Data
@ApiModel(description = "小组查询DTO")
public class GroupQueryDTO {

    @ApiModelProperty(value = "页码（默认1）")
    private Integer pageNum = 1;

    @ApiModelProperty(value = "每页条数（默认10）")
    private Integer pageSize = 10;

    @ApiModelProperty(value = "小组名称（模糊查询）")
    private String groupName;

    @ApiModelProperty(value = "角色ID（查询指定角色下的小组）")
    private String roleId;

    @ApiModelProperty(value = "状态（1-启用，0-禁用）")
    private Integer status;
}