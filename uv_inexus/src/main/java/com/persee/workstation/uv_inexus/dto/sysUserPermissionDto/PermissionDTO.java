package com.persee.workstation.uv_inexus.dto.sysUserPermissionDto;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 权限信息DTO（包含所有字段）
 */
@Data
@ApiModel("权限信息（全字段）")
public class PermissionDTO {
    @ApiModelProperty("权限ID")
    private String id;

    @ApiModelProperty("权限名称")
    private String permName;

    @ApiModelProperty("标题名称")
    private String title;

    @ApiModelProperty("权限编码（如：user:add）")
    private String permCode;

    @ApiModelProperty("权限类型：1-菜单路由权限，2-数据权限，3-操作权限")
    private Integer permType;

    @ApiModelProperty("父权限ID，用于构建权限层级")
    private Integer parentId;

    @ApiModelProperty("路由路径，仅菜单权限需要")
    private String path;

    @ApiModelProperty("前端组件路径，仅菜单权限需要")
    private String component;

    @ApiModelProperty("菜单图标，仅菜单权限需要")
    private String icon;

    @ApiModelProperty("排序号")
    private Integer sort;

    @ApiModelProperty("状态：1-启用，0-禁用")
    private Integer status;

    @ApiModelProperty("权限描述")
    private String description;

    @ApiModelProperty("是否隐藏")
    private Integer hidden;

    // 如果权限表有创建时间、修改时间等字段，也一并添加
    @ApiModelProperty("创建时间")
    private String createTime;

    @ApiModelProperty("更新时间")
    private String updateTime;

    // 新增：子权限列表（核心字段，实现树形结构）
    private List<PermissionDTO> children;
}