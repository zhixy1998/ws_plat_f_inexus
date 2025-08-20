package com.persee.workstation.uv_inexus.dto.sysUserPermissionDto;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 组-用户树形结构DTO
 */
@Data
@ApiModel("小组树形结构")
public class GroupTreeDTO {
    @ApiModelProperty("小组ID")
    private String id;

    @ApiModelProperty("小组名称")
    private String groupName;

    @ApiModelProperty("小组描述")
    private String description;

    @ApiModelProperty("小组状态（1-启用，0-禁用）")
    private Integer status;

    @ApiModelProperty("该小组下的用户列表")
    private List<UserTreeDTO> userList;  // 同级包含用户列表
}