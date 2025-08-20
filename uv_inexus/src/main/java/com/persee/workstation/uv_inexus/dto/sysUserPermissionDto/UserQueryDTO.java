package com.persee.workstation.uv_inexus.dto.sysUserPermissionDto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 用户分页查询数据传输对象
 */
@Data
@ApiModel(description = "用户查询DTO")
public class UserQueryDTO {

    @ApiModelProperty(value = "页码（默认1）")
    private Integer pageNum = 1;

    @ApiModelProperty(value = "每页条数（默认10）")
    private Integer pageSize = 10;

    @ApiModelProperty(value = "用户名（模糊查询）")
    private String username;

    @ApiModelProperty(value = "状态（1-启用，0-禁用）")
    private Integer status;
}