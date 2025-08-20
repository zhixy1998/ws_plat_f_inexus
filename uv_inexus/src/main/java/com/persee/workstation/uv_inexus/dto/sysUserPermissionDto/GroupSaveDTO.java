package com.persee.workstation.uv_inexus.dto.sysUserPermissionDto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 小组新增/修改数据传输对象
 */
@Data
@ApiModel(description = "小组新增/修改DTO")
public class GroupSaveDTO {

    @ApiModelProperty(value = "小组ID（修改时必传，新增时不传）")
    private String id;

    @ApiModelProperty(value = "小组名称", required = true)
    @NotBlank(message = "小组名称不能为空")
    @Size(max = 50, message = "小组名称长度不能超过50个字符")
    private String groupName;

    @ApiModelProperty(value = "角色ID（关联的角色）", required = true)
    @NotBlank(message = "角色ID不能为空")
    private String roleId;

    @ApiModelProperty(value = "小组描述")
    private String description;

    @ApiModelProperty(value = "状态（1-启用，0-禁用）", required = true)
    private Integer status;
}