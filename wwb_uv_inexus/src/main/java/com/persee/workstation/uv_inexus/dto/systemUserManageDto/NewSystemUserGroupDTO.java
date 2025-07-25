package com.persee.workstation.uv_inexus.dto.systemUserManageDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class NewSystemUserGroupDTO {
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

    @ApiModelProperty("系统权限列表")
    private List<Integer> groupPermissionList;

    @ApiModelProperty("当前用户")
    private String updateBy;
}
