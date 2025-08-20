package com.persee.workstation.uv_inexus.dto.sysUserPermissionDto;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 角色-组-用户树形结构DTO
 */
@Data
@ApiModel("角色树形结构")
public class RoleTreeDTO {
    @ApiModelProperty("角色ID")
    private String id;

    @ApiModelProperty("角色名称")
    private String roleName;

    @ApiModelProperty("角色编码")
    private String roleCode;

    @ApiModelProperty("角色状态（1-启用，0-禁用）")
    private Integer status;

    @ApiModelProperty("该角色下的小组列表")
    private List<GroupTreeDTO> groupList;  // 同级包含组列表
}