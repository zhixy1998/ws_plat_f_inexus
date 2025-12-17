package com.persee.workstation.uv_inexus.dto.sysUserPermissionDto;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "角色权限更新DTO")
public class RolePermissionUpdateDTO {

    @ApiModelProperty(value = "角色ID", required = true)
    @NotBlank(message = "角色ID不能为空")
    private String roleId;

    @ApiModelProperty(value = "权限ID集合", required = false)
    @NotEmpty(message = "权限ID集合不能为空")
    private List<String> permIds;
}

