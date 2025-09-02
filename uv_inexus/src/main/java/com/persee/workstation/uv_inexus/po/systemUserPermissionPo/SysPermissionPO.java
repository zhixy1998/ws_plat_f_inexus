package com.persee.workstation.uv_inexus.po.systemUserPermissionPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限表PO类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("权限表")
@TableName("t_sys_permission")
public class SysPermissionPO extends BasePO {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("权限ID")
    private String id;

    @TableField("title")
    @ApiModelProperty("路由路径，仅菜单权限需要")
    private String title;

    @TableField("perm_name")
    @ApiModelProperty("权限名称")
    private String permName;

    @TableField("perm_code")
    @ApiModelProperty("权限编码")
    private String permCode;

    @TableField("perm_type")
    @ApiModelProperty("权限类型：1-菜单路由权限，2-操作权限，3-数据权限")
    private Integer permType;

    @TableField("parent_id")
    @ApiModelProperty("父权限ID，用于构建权限层级")
    private Integer parentId;

    @TableField("path")
    @ApiModelProperty("路由路径，仅菜单权限需要")
    private String path;

    @TableField("redirect")
    @ApiModelProperty("重定向")
    private String redirect;

    @TableField("component")
    @ApiModelProperty("前端组件路径，仅菜单权限需要")
    private String component;

    @TableField("icon")
    @ApiModelProperty("菜单图标，仅菜单权限需要")
    private String icon;

    @TableField("sort")
    @ApiModelProperty("排序号")
    private Integer sort;

    @TableField("status")
    @ApiModelProperty("状态：1-启用，0-禁用")
    private Integer status;

    // 补充 hidden 字段，对应数据库表可能有的 hidden 字段，若没有需确认是否要新增
    @TableField("hidden") 
    @ApiModelProperty("是否隐藏：1-是，0-否")
    private Integer hidden;

    // 补充 noAuth 字段，对应数据库表可能有的 no_auth 字段，若没有需确认是否要新增
    @TableField("no_auth") 
    @ApiModelProperty("是否无需权限：1-是，0-否")
    private Integer noAuth; 
}
