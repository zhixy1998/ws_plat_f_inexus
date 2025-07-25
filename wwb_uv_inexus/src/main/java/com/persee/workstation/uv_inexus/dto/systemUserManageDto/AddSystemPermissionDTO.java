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
