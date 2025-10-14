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
 * 小组-用户树形结构DTO（父：小组，子：用户）
 */
@Data
@ApiModel("小组-用户树形结构DTO")
public class RoleGroupUserTreeDTO {
    // -------------------------- 小组信息（父节点） --------------------------
    @ApiModelProperty("小组ID")
    private String groupId;

    @ApiModelProperty("小组名称")
    private String groupName;

    @ApiModelProperty("小组状态（1-启用，0-禁用）")
    private Integer groupStatus;

    // -------------------------- 小组下的用户列表（子节点） --------------------------
    @ApiModelProperty("小组内的用户列表")
    private List<UserInfoDTO> children;


    /**
     * 子节点：用户信息DTO
     */
    @Data
    @ApiModel("用户信息子节点DTO")
    public static class UserInfoDTO {
        @ApiModelProperty("用户ID")
        private String userId;

        @ApiModelProperty("用户名")
        private String username;

        @ApiModelProperty("真实姓名")
        private String realName;

        @ApiModelProperty("手机号")
        private String phone;

        @ApiModelProperty("邮箱")
        private String email;

        @ApiModelProperty("用户状态（1-启用，0-禁用）")
        private Integer userStatus;
    }
}