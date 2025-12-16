package com.persee.workstation.uv_inexus.dto.sysUserPermissionDto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 权限树查询参数DTO
 */
@Data
@ApiModel(value = "PermissionTreeQueryDTO", description = "权限树查询参数")
public class PermissionTreeQueryDTO {

    @ApiModelProperty(value = "权限类型，多个用逗号分隔，如0,1,2，null表示所有类型")
    private String permType;

    @ApiModelProperty(value = "角色ID", required = true)
    private String roleId;
}
