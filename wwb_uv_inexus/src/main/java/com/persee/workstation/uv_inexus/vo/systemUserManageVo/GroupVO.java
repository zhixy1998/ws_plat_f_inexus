package com.persee.workstation.uv_inexus.vo.systemUserManageVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class GroupVO {

    @ApiModelProperty("组名称")
    private String groupName;

    @ApiModelProperty("组编号")
    private String groupNo;

    @ApiModelProperty("组描述")
    private String groupDesc;

    @ApiModelProperty("用户")
    private List<SystemUserInfoVO> list;
}
