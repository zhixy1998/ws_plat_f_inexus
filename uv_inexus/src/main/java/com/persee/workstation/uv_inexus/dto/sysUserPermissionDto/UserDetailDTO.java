package com.persee.workstation.uv_inexus.dto.sysUserPermissionDto;

import java.time.LocalDateTime;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 用户详情DTO（含组、角色信息）
 */
@Data
@ApiModel("用户详情DTO")
public class UserDetailDTO {

    @ApiModelProperty("用户ID")
    private String id;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("真实姓名")
    private String realName;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("职务")
    private String position;

    @ApiModelProperty("备注")
    private String remarks;

    @ApiModelProperty("用户状态：1-启用，0-禁用")
    private Integer status;

    @ApiModelProperty("最后登录时间")
    private LocalDateTime lastLoginTime;

    // -------------- 所属组信息 --------------
    @ApiModelProperty("所属组ID")
    private String groupId;

    @ApiModelProperty("所属组名称")
    private String groupName;

    @ApiModelProperty("组描述")
    private String groupDescription;

    // -------------- 角色信息 --------------
    @ApiModelProperty("角色ID")
    private String roleId;

    @ApiModelProperty("角色名称")
    private String roleName;

    @ApiModelProperty("角色编码")
    private String roleCode;

    @ApiModelProperty("角色描述")
    private String roleDescription;
}