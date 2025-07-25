package com.persee.workstation.uv_inexus.dto.systemUserManageDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class CreateSystemUserDTO {

    @ApiModelProperty("用户名")
    private String userName;

    @ApiModelProperty("用户密码")
    private String password;

    @ApiModelProperty("用户真实姓名")
    private String userRealName;

    @ApiModelProperty("电话")
    private String phone;

    @ApiModelProperty("用户邮箱")
    private String email;

    @ApiModelProperty("用户职位")
    private String userPosition;

    @ApiModelProperty("用户备注")
    private String remark;

    @ApiModelProperty("组名")
    private String groupName;

    @ApiModelProperty("用户组编号")
    private String groupNo;

    @ApiModelProperty("系统标识id")
    private String systemIdentityId;

    @ApiModelProperty("用户是否有效")
    private Boolean isValid;

    @ApiModelProperty("用户权限列表")
    private List<Integer> userPermissionList;

    @ApiModelProperty("修改者")
    private String updateBy;
}
