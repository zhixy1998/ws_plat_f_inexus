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
