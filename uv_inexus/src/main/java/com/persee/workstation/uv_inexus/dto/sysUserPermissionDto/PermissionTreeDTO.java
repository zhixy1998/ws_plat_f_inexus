package com.persee.workstation.uv_inexus.dto.sysUserPermissionDto;

import java.util.List;

import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysPermissionPO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限树形结构DTO（包含权限表所有字段+角色权限标记+子权限）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("权限树形结构DTO")
public class PermissionTreeDTO extends SysPermissionPO {

    @ApiModelProperty("角色是否拥有该权限：true=拥有，false=未拥有")
    private Boolean isHave;

    @ApiModelProperty("子权限列表（构建树形结构）")
    private List<PermissionTreeDTO> children;
}