package com.persee.workstation.uv_inexus.vo.systemUserManageVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemUserInfoVO {

    @ApiModelProperty("用户名")
    private String userName;

    @ApiModelProperty("用户真实姓名")
    private String userRealName;

    @ApiModelProperty("电话")
    private String phone;

    @ApiModelProperty("用户邮箱")
    private String email;

    @ApiModelProperty("用户职位")
    private String userPosition;

    @ApiModelProperty("组名")
    private String groupName;

    @ApiModelProperty("用户组编号")
    private String groupNo;

    @ApiModelProperty("系统角色")
    private String systemIdentityName;

    @ApiModelProperty("系统角色id")
    private String systemIdentityId;
}
