package com.persee.workstation.uv_inexus.vo.systemUserManageVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class QueryUserVO {

    @ApiModelProperty("系统角色名")
    private String systemIdentityName;

    @ApiModelProperty("系统角色id")
    private String systemIdentityId;

    @ApiModelProperty("用户组")
    private List<GroupVO> list;

}
